import { Injectable } from '@angular/core';
import { StateStorageService } from './state-storage.service';

@Injectable({ providedIn: 'root' })
export class TabTrackerService {
  private readonly currentTabIdKey = 'current-tab-id'; // Unique ID for this specific tab instance in sessionStorage
  private readonly activeTabsKey = 'active-tabs'; // Set of all active tab IDs in localStorage
  private tabId: string;

  constructor(private stateStorageService: StateStorageService) {
    // 1. Get or generate a unique ID for this tab session
    let storedTabId = sessionStorage.getItem(this.currentTabIdKey);
    if (!storedTabId) {
      storedTabId = `tab-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
      sessionStorage.setItem(this.currentTabIdKey, storedTabId);
    }
    this.tabId = storedTabId;

    // 2. Register this tab
    this.registerTab();

    // 3. Setup event listeners
    this.setupEventListeners();

    // If this is a reload, clear the reloading flag after a short delay
    // This ensures the flag is true during initial HTTP requests
    if (this.stateStorageService.isReloading()) {
      setTimeout(() => {
        this.stateStorageService.clearReloadingFlag();
      }, 200); // 200ms delay to allow interceptors to check the flag
    }
  }

  private getActiveTabs(): Set<string> {
    const storedTabs = localStorage.getItem(this.activeTabsKey);
    return storedTabs ? new Set(JSON.parse(storedTabs)) : new Set();
  }

  private saveActiveTabs(tabs: Set<string>): void {
    localStorage.setItem(this.activeTabsKey, JSON.stringify(Array.from(tabs)));
  }

  private registerTab(): void {
    const activeTabs = this.getActiveTabs();
    activeTabs.add(this.tabId);
    this.saveActiveTabs(activeTabs);
    // No longer clearing reloading flag here, constructor handles it with delay
  }

  private unregisterTab(): void {
    const activeTabs = this.getActiveTabs();
    activeTabs.delete(this.tabId);
    this.saveActiveTabs(activeTabs);

    if (activeTabs.size === 0) {
      // If this was the last tab, and it's not just reloading, then logout
      if (!this.stateStorageService.isReloading()) {
        this.stateStorageService.clearAuthenticationToken();
      }
    }
  }

  private setupEventListeners(): void {
    window.addEventListener('beforeunload', () => {
      // Set a flag indicating the tab is reloading
      this.stateStorageService.setReloadingFlag();
      this.unregisterTab();
    });

    // Remove the 'load' event listener as constructor now handles clearing the flag with delay
    // window.addEventListener('load', () => {
    //   this.stateStorageService.clearReloadingFlag();
    // });
  }
}

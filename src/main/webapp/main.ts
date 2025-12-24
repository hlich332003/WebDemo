(window as any).global = window;
import('./bootstrap').catch((err: unknown) => console.error(err));

jest.mock('app/core/auth/account.service');
jest.mock('app/login/login.service');

import { ElementRef } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { Navigation, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AccountService } from 'app/core/auth/account.service';

import { LoginService } from './login.service';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let comp: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let mockRouter: Router;
  let mockAccountService: AccountService;
  let mockLoginService: LoginService;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        FormBuilder,
        AccountService,
        {
          provide: LoginService,
          useValue: {
            login: jest.fn(() => of({})),
          },
        },
      ],
    })
      .overrideTemplate(LoginComponent, '')
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LoginComponent);
    comp = fixture.componentInstance;
    mockRouter = TestBed.inject(Router);
    jest.spyOn(mockRouter, 'navigate').mockImplementation(() => Promise.resolve(true));
    mockLoginService = TestBed.inject(LoginService);
    mockAccountService = TestBed.inject(AccountService);
  });

  // Đã xóa describe('ngOnInit') vì LoginComponent không có ngOnInit

  describe('ngAfterViewInit', () => {
    it('should set focus to username input after the view has been initialized', () => {
      // GIVEN
      const node = {
        focus: jest.fn(),
      };

      // Mock ViewChild
      comp.username = new ElementRef(node);

      // WHEN
      comp.ngAfterViewInit();

      // THEN
      expect(node.focus).toHaveBeenCalled();
    });
  });

  describe('login', () => {
    it('should authenticate the user and navigate to home page', () => {
      // GIVEN
      const credentials = {
        username: 'admin',
        password: 'admin',
        rememberMe: true,
      };

      comp.loginForm.patchValue({
        username: 'admin',
        password: 'admin',
        rememberMe: true,
      });

      // Mock identity to return account
      mockAccountService.identity = jest.fn(() => of({ authorities: [] } as any));

      // WHEN
      comp.login();

      // THEN
      expect(comp.authenticationError).toEqual(false); // Sửa: truy cập property trực tiếp
      expect(mockLoginService.login).toHaveBeenCalledWith(credentials);
      expect(mockRouter.navigate).toHaveBeenCalledWith(['']);
    });

    it('should stay on login form and show error message on login error', () => {
      // GIVEN
      mockLoginService.login = jest.fn(() => throwError(() => new Error('')));

      // WHEN
      comp.login();

      // THEN
      expect(comp.authenticationError).toEqual(true); // Sửa: truy cập property trực tiếp
      expect(mockRouter.navigate).not.toHaveBeenCalled();
    });
  });
});

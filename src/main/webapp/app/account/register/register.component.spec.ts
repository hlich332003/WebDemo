jest.mock('app/login/login.service');
jest.mock('./register.service');

import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';

import { LoginService } from 'app/login/login.service';

import { RegisterService } from './register.service';
import RegisterComponent from './register.component';

describe('RegisterComponent', () => {
  let comp: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let mockRegisterService: RegisterService;
  let mockLoginService: LoginService;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [FormBuilder, RegisterService, LoginService],
    })
      .overrideTemplate(RegisterComponent, '')
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RegisterComponent);
    comp = fixture.componentInstance;
    mockRegisterService = TestBed.inject(RegisterService);
    mockLoginService = TestBed.inject(LoginService);
  });

  describe('register', () => {
    it('should set success to true on successful registration', () => {
      // GIVEN
      mockRegisterService.save = jest.fn(() => of({}));
      // Sửa mock login trả về null để khớp kiểu Account | null
      mockLoginService.login = jest.fn(() => of(null));
      comp.registerForm.patchValue({
        email: 'user@example.com',
        password: 'password',
        confirmPassword: 'password',
      });

      // WHEN
      comp.register();

      // THEN
      expect(comp.success()).toBe(true);
    });

    it('should set errorEmailExists to true on email already used', () => {
      // GIVEN
      mockRegisterService.save = jest.fn(() =>
        throwError(
          () =>
            new HttpErrorResponse({
              status: 400,
              error: { type: 'EMAIL_ALREADY_USED' },
            }),
        ),
      );
      comp.registerForm.patchValue({
        email: 'user@example.com',
        password: 'password',
        confirmPassword: 'password',
      });

      // WHEN
      comp.register();

      // THEN
      expect(comp.errorEmailExists()).toBe(true);
    });

    it('should set error to true on general error', () => {
      // GIVEN
      mockRegisterService.save = jest.fn(() => throwError(() => new HttpErrorResponse({ status: 500 })));
      comp.registerForm.patchValue({
        email: 'user@example.com',
        password: 'password',
        confirmPassword: 'password',
      });

      // WHEN
      comp.register();

      // THEN
      expect(comp.error()).toBe(true);
    });

    it('should set doNotMatch to true on password mismatch', () => {
      // GIVEN
      comp.registerForm.patchValue({
        email: 'user@example.com',
        password: 'password',
        confirmPassword: 'wrongpassword',
      });

      // WHEN
      comp.register();

      // THEN
      expect(comp.doNotMatch()).toBe(true);
    });
  });
});

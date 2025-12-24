import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { CartService } from 'app/shared/services/cart.service'; // Sửa đường dẫn import
import { ApplicationConfigService } from 'app/core/config/application-config.service';

describe('Cart Service', () => {
  let service: CartService;
  let httpMock: HttpTestingController;
  let applicationConfigService: ApplicationConfigService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ApplicationConfigService],
    });
    service = TestBed.inject(CartService);
    httpMock = TestBed.inject(HttpTestingController);
    applicationConfigService = TestBed.inject(ApplicationConfigService);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Service methods', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });
  });
});

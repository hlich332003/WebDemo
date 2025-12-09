import { TestBed } from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { Account } from 'app/core/auth/account.model'; // Đã sửa import
import {
  sampleWithFullData,
  sampleWithPartialData,
  sampleWithRequiredData,
} from '../user.test-samples';

import { UserService } from './user.service';

const requireRestSample: Account = {
  ...sampleWithRequiredData,
};

describe('User Service', () => {
  let service: UserService;
  let httpMock: HttpTestingController;
  let expectedResult: Account | Account[] | boolean | null; // Đã sửa kiểu

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  describe('Service methods', () => {
    it('should find an element', () => {
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.find(123).subscribe((resp) => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of User', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe((resp) => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    describe('addUserToCollectionIfMissing', () => {
      it('should add a User to an empty array', () => {
        const user: Account = sampleWithRequiredData; // Đã sửa kiểu
        expectedResult = service.addUserToCollectionIfMissing([], user);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(user);
      });

      it('should not add a User to an array that contains it', () => {
        const user: Account = sampleWithRequiredData; // Đã sửa kiểu
        const userCollection: Account[] = [{ ...user }, sampleWithPartialData];
        expectedResult = service.addUserToCollectionIfMissing(
          userCollection,
          user,
        );
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a User to an array that doesn't contain it", () => {
        const user: Account = sampleWithRequiredData; // Đã sửa kiểu
        const userCollection: Account[] = [sampleWithPartialData];
        expectedResult = service.addUserToCollectionIfMissing(
          userCollection,
          user,
        );
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(user);
      });

      it('should add only unique User to an array', () => {
        const userArray: Account[] = [
          sampleWithRequiredData,
          sampleWithPartialData,
          sampleWithFullData,
        ]; // Đã sửa kiểu
        const userCollection: Account[] = [sampleWithRequiredData];
        expectedResult = service.addUserToCollectionIfMissing(
          userCollection,
          ...userArray,
        );
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const user: Account = sampleWithRequiredData; // Đã sửa kiểu
        const user2: Account = sampleWithPartialData; // Đã sửa kiểu
        expectedResult = service.addUserToCollectionIfMissing([], user, user2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(user);
        expect(expectedResult).toContain(user2);
      });

      it('should accept null and undefined values', () => {
        const user: Account = sampleWithRequiredData; // Đã sửa kiểu
        expectedResult = service.addUserToCollectionIfMissing(
          [],
          null,
          user,
          undefined,
        );
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(user);
      });

      it('should return initial array if no User is added', () => {
        const userCollection: Account[] = [sampleWithRequiredData]; // Đã sửa kiểu
        expectedResult = service.addUserToCollectionIfMissing(
          userCollection,
          undefined,
          null,
        );
        expect(expectedResult).toEqual(userCollection);
      });
    });

    describe('compareUser', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareUser(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 3944 };
        const entity2 = null;

        const compareResult1 = service.compareUser(entity1, entity2);
        const compareResult2 = service.compareUser(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 3944 };
        const entity2 = { id: 6275 };

        const compareResult1 = service.compareUser(entity1, entity2);
        const compareResult2 = service.compareUser(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey matches', () => {
        const entity1 = { id: 3944 };
        const entity2 = { id: 3944 };

        const compareResult1 = service.compareUser(entity1, entity2);
        const compareResult2 = service.compareUser(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});

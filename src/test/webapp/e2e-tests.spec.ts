/// <reference types="cypress" />
/**
 * E2E TEST SUITE - WEBDEMO E-COMMERCE
 * Automated End-to-End Testing
 */

describe('WebDemo E-Commerce E2E Tests', () => {
  const baseUrl = 'http://localhost:8080';

  const testUser = { username: 'user', password: 'user' };
  const testAdmin = { username: 'admin', password: 'admin' };

  beforeEach(() => {
    localStorage.clear();
  });

  describe('Authentication', () => {
    it('TC-AUTH-001: Login as regular user', () => {
      cy.visit(`${baseUrl}/login`);
      cy.get('input[name="username"]').type(testUser.username);
      cy.get('input[name="password"]').type(testUser.password);
      cy.get('button[type="submit"]').click();
      cy.url().should('eq', `${baseUrl}/`);
    });

    it('TC-AUTH-002: Login as admin', () => {
      cy.visit(`${baseUrl}/login`);
      cy.get('input[name="username"]').type(testAdmin.username);
      cy.get('input[name="password"]').type(testAdmin.password);
      cy.get('button[type="submit"]').click();
      cy.url().should('include', '/admin');
    });

    it('TC-AUTH-003: Fail login with wrong password', () => {
      cy.visit(`${baseUrl}/login`);
      cy.get('input[name="username"]').type(testAdmin.username);
      cy.get('input[name="password"]').type('wrongpassword');
      cy.get('button[type="submit"]').click();
      cy.get('.alert-danger').should('be.visible');
    });
  });

  describe('Shopping Cart', () => {
    it('TC-CART-001: Add product to cart', () => {
      cy.visit(`${baseUrl}/products`);
      cy.get('.product-card').first().find('button').contains('Thêm vào giỏ').click();
      cy.get('.alert-success').should('contain', 'Đã thêm sản phẩm vào giỏ hàng');
    });

    it('TC-CART-007: Persist cart in localStorage', () => {
      cy.visit(`${baseUrl}/products`);
      cy.get('.product-card').first().find('button').contains('Thêm vào giỏ').click();
      cy.reload();
      cy.get('.cart-badge').should('contain', '1');
    });
  });
});

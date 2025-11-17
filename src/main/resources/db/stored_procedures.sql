-- =============================================
-- Stored Procedure: Lấy top sản phẩm bán chạy
-- =============================================
CREATE PROCEDURE sp_GetTopSellingProducts
    @TopN INT = 10
AS
BEGIN
    SET NOCOUNT ON;
    
    SELECT TOP (@TopN)
        p.id,
        p.name,
        p.price,
        p.image_url,
        COUNT(oi.id) as total_orders,
        SUM(oi.quantity) as total_quantity_sold
    FROM jhi_product p
    INNER JOIN jhi_order_item oi ON p.id = oi.product_id
    GROUP BY p.id, p.name, p.price, p.image_url
    ORDER BY total_quantity_sold DESC;
END;
GO

-- =============================================
-- Stored Procedure: Thống kê doanh thu theo tháng
-- =============================================
CREATE PROCEDURE sp_GetRevenueByMonth
    @Year INT,
    @Month INT
AS
BEGIN
    SET NOCOUNT ON;
    
    SELECT 
        YEAR(o.created_date) as year,
        MONTH(o.created_date) as month,
        COUNT(DISTINCT o.id) as total_orders,
        SUM(o.total_amount) as total_revenue,
        AVG(o.total_amount) as avg_order_value
    FROM jhi_order o
    WHERE YEAR(o.created_date) = @Year 
        AND MONTH(o.created_date) = @Month
        AND o.status = 'COMPLETED'
    GROUP BY YEAR(o.created_date), MONTH(o.created_date);
END;
GO

-- =============================================
-- Stored Procedure: Cập nhật số lượng sản phẩm
-- =============================================
CREATE PROCEDURE sp_UpdateProductQuantity
    @ProductId BIGINT,
    @Quantity INT,
    @Result INT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;
    
    BEGIN TRY
        DECLARE @CurrentQuantity INT;
        
        SELECT @CurrentQuantity = quantity 
        FROM jhi_product 
        WHERE id = @ProductId;
        
        IF @CurrentQuantity < @Quantity
        BEGIN
            SET @Result = -1;
            ROLLBACK TRANSACTION;
            RETURN;
        END
        
        UPDATE jhi_product 
        SET quantity = quantity - @Quantity,
            last_modified_date = GETDATE()
        WHERE id = @ProductId;
        
        SET @Result = 1;
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        SET @Result = 0;
        ROLLBACK TRANSACTION;
    END CATCH
END;
GO

USE [jhipster_db]
GO
/****** Object:  User [jhipster_user]    Script Date: 12/3/2025 10:15:54 AM ******/
CREATE USER [jhipster_user] FOR LOGIN [jhipster_user] WITH DEFAULT_SCHEMA=[dbo]
GO
ALTER ROLE [db_owner] ADD MEMBER [jhipster_user]
GO
/****** Object:  Table [dbo].[jhi_authority]    Script Date: 12/3/2025 10:15:54 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[jhi_authority](
  [name] [nvarchar](50) NOT NULL,
  PRIMARY KEY CLUSTERED
(
[name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
  ) ON [PRIMARY]
  GO
/****** Object:  Table [dbo].[jhi_cart]    Script Date: 12/3/2025 10:15:54 AM ******/
  SET ANSI_NULLS ON
  GO
  SET QUOTED_IDENTIFIER ON
  GO
CREATE TABLE [dbo].[jhi_cart](
  [id] [bigint] IDENTITY(1,1) NOT NULL,
  [user_id] [bigint] NOT NULL,
  [created_date] [datetimeoffset](6) NOT NULL,
  [updated_date] [datetimeoffset](6) NULL,
  PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
  ) ON [PRIMARY]
  GO
/****** Object:  Table [dbo].[jhi_cart_item]    Script Date: 12/3/2025 10:15:54 AM ******/
  SET ANSI_NULLS ON
  GO
  SET QUOTED_IDENTIFIER ON
  GO
CREATE TABLE [dbo].[jhi_cart_item](
  [id] [bigint] IDENTITY(1,1) NOT NULL,
  [cart_id] [bigint] NOT NULL,
  [product_id] [bigint] NOT NULL,
  [quantity] [int] NOT NULL,
  PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
  ) ON [PRIMARY]
  GO
/****** Object:  Table [dbo].[jhi_category]    Script Date: 12/3/2025 10:15:54 AM ******/
  SET ANSI_NULLS ON
  GO
  SET QUOTED_IDENTIFIER ON
  GO
CREATE TABLE [dbo].[jhi_category](
  [id] [bigint] IDENTITY(1,1) NOT NULL,
  [name] [nvarchar](50) NOT NULL,
  [slug] [nvarchar](50) NOT NULL,
  [created_by] [nvarchar](50) NOT NULL,
  [created_date] [datetimeoffset](6) NOT NULL,
  [last_modified_by] [nvarchar](50) NULL,
  [last_modified_date] [datetimeoffset](6) NULL,
  PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
  UNIQUE NONCLUSTERED
(
[slug] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
  UNIQUE NONCLUSTERED
(
[name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
  ) ON [PRIMARY]
  GO
/****** Object:  Table [dbo].[jhi_order]    Script Date: 12/3/2025 10:15:54 AM ******/
  SET ANSI_NULLS ON
  GO
  SET QUOTED_IDENTIFIER ON
  GO
CREATE TABLE [dbo].[jhi_order](
  [id] [bigint] IDENTITY(1,1) NOT NULL,
  [user_id] [bigint] NOT NULL,
  [customer_email] [nvarchar](255) NULL,
  [customer_full_name] [nvarchar](255) NULL,
  [customer_phone] [nvarchar](10) NULL,
  [delivery_address] [nvarchar](255) NULL,
  [order_date] [datetimeoffset](6) NOT NULL,
  [payment_method] [nvarchar](50) NULL,
  [status] [nvarchar](50) NULL,
  [total_amount] [decimal](18, 2) NULL,
  [notes] [nvarchar](500) NULL,
  [order_code] [nvarchar](255) NOT NULL,
  PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
  ) ON [PRIMARY]
  GO
/****** Object:  Table [dbo].[jhi_order_item]    Script Date: 12/3/2025 10:15:54 AM ******/
  SET ANSI_NULLS ON
  GO
  SET QUOTED_IDENTIFIER ON
  GO
CREATE TABLE [dbo].[jhi_order_item](
  [id] [bigint] IDENTITY(1,1) NOT NULL,
  [order_id] [bigint] NOT NULL,
  [product_id] [bigint] NOT NULL,
  [product_name] [nvarchar](255) NULL,
  [quantity] [int] NOT NULL,
  [price] [decimal](18, 2) NOT NULL,
  [price_at_purchase] [decimal](18, 2) NULL,
  PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
  ) ON [PRIMARY]
  GO
/****** Object:  Table [dbo].[jhi_payment]    Script Date: 12/3/2025 10:15:54 AM ******/
  SET ANSI_NULLS ON
  GO
  SET QUOTED_IDENTIFIER ON
  GO
CREATE TABLE [dbo].[jhi_payment](
  [id] [bigint] IDENTITY(1,1) NOT NULL,
  [order_id] [bigint] NOT NULL,
  [method] [nvarchar](50) NULL,
  [status] [nvarchar](50) NULL,
  [paid_at] [datetimeoffset](6) NULL,
  [amount] [decimal](18, 2) NULL,
  PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
  ) ON [PRIMARY]
  GO
/****** Object:  Table [dbo].[jhi_product]    Script Date: 12/3/2025 10:15:54 AM ******/
  SET ANSI_NULLS ON
  GO
  SET QUOTED_IDENTIFIER ON
  GO
CREATE TABLE [dbo].[jhi_product](
  [id] [bigint] IDENTITY(1,1) NOT NULL,
  [name] [nvarchar](100) NOT NULL,
  [description] [nvarchar](500) NULL,
  [image_url] [nvarchar](255) NULL,
  [price] [decimal](18, 2) NOT NULL,
  [quantity] [int] NOT NULL,
  [is_active] [bit] NOT NULL,
  [category_id] [bigint] NOT NULL,
  [created_by] [nvarchar](50) NOT NULL,
  [created_date] [datetimeoffset](6) NOT NULL,
  [last_modified_by] [nvarchar](50) NULL,
  [last_modified_date] [datetimeoffset](6) NULL,
  [sales_count] [bigint] NOT NULL,
  PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
  ) ON [PRIMARY]
  GO
/****** Object:  Table [dbo].[jhi_refresh_token]    Script Date: 12/3/2025 10:15:54 AM ******/
  SET ANSI_NULLS ON
  GO
  SET QUOTED_IDENTIFIER ON
  GO
CREATE TABLE [dbo].[jhi_refresh_token](
  [id] [bigint] IDENTITY(1,1) NOT NULL,
  [user_id] [bigint] NOT NULL,
  [token] [nvarchar](255) NOT NULL,
  [expiry_date] [datetimeoffset](6) NOT NULL,
  PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
  UNIQUE NONCLUSTERED
(
[token] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
  ) ON [PRIMARY]
  GO
/****** Object:  Table [dbo].[jhi_review]    Script Date: 12/3/2025 10:15:54 AM ******/
  SET ANSI_NULLS ON
  GO
  SET QUOTED_IDENTIFIER ON
  GO
CREATE TABLE [dbo].[jhi_review](
  [id] [bigint] IDENTITY(1,1) NOT NULL,
  [user_id] [bigint] NOT NULL,
  [product_id] [bigint] NOT NULL,
  [rating] [int] NOT NULL,
  [comment] [nvarchar](1000) NULL,
  [created_date] [datetimeoffset](6) NOT NULL,
  PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
  ) ON [PRIMARY]
  GO
/****** Object:  Table [dbo].[jhi_user]    Script Date: 12/3/2025 10:15:54 AM ******/
  SET ANSI_NULLS ON
  GO
  SET QUOTED_IDENTIFIER ON
  GO
CREATE TABLE [dbo].[jhi_user](
  [id] [bigint] IDENTITY(1,1) NOT NULL,
  [password_hash] [nvarchar](60) NULL,
  [email] [nvarchar](254) NULL,
  [first_name] [nvarchar](50) NULL,
  [last_name] [nvarchar](50) NULL,
  [phone] [nvarchar](10) NULL,
  [image_url] [nvarchar](256) NULL,
  [activated] [bit] NOT NULL,
  [created_by] [nvarchar](50) NOT NULL,
  [created_date] [datetimeoffset](6) NOT NULL,
  [last_modified_by] [nvarchar](50) NULL,
  [last_modified_date] [datetimeoffset](6) NULL,
  [activation_key] [nvarchar](20) NULL,
  [lang_key] [nvarchar](10) NULL,
  [reset_date] [datetimeoffset](6) NULL,
  [reset_key] [nvarchar](20) NULL,
  [authority_name] [nvarchar](50) NOT NULL,
  PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
  UNIQUE NONCLUSTERED
(
[email] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
  UNIQUE NONCLUSTERED
(
[phone] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
  ) ON [PRIMARY]
  GO
/****** Object:  Table [dbo].[jhi_user_authority]    Script Date: 12/3/2025 10:15:54 AM ******/
  SET ANSI_NULLS ON
  GO
  SET QUOTED_IDENTIFIER ON
  GO
CREATE TABLE [dbo].[jhi_user_authority](
  [user_id] [bigint] NOT NULL,
  [authority_name] [nvarchar](50) NOT NULL,
  PRIMARY KEY CLUSTERED
(
  [user_id] ASC,
[authority_name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
  ) ON [PRIMARY]
  GO
ALTER TABLE [dbo].[jhi_cart]  WITH CHECK ADD FOREIGN KEY([user_id])
  REFERENCES [dbo].[jhi_user] ([id])
  GO
ALTER TABLE [dbo].[jhi_cart_item]  WITH CHECK ADD FOREIGN KEY([cart_id])
  REFERENCES [dbo].[jhi_cart] ([id])
  GO
ALTER TABLE [dbo].[jhi_cart_item]  WITH CHECK ADD FOREIGN KEY([product_id])
  REFERENCES [dbo].[jhi_product] ([id])
  GO
ALTER TABLE [dbo].[jhi_order]  WITH CHECK ADD FOREIGN KEY([user_id])
  REFERENCES [dbo].[jhi_user] ([id])
  GO
ALTER TABLE [dbo].[jhi_order_item]  WITH CHECK ADD FOREIGN KEY([order_id])
  REFERENCES [dbo].[jhi_order] ([id])
  GO
ALTER TABLE [dbo].[jhi_order_item]  WITH CHECK ADD FOREIGN KEY([product_id])
  REFERENCES [dbo].[jhi_product] ([id])
  GO
ALTER TABLE [dbo].[jhi_payment]  WITH CHECK ADD FOREIGN KEY([order_id])
  REFERENCES [dbo].[jhi_order] ([id])
  GO
ALTER TABLE [dbo].[jhi_product]  WITH CHECK ADD FOREIGN KEY([category_id])
  REFERENCES [dbo].[jhi_category] ([id])
  GO
ALTER TABLE [dbo].[jhi_refresh_token]  WITH CHECK ADD FOREIGN KEY([user_id])
  REFERENCES [dbo].[jhi_user] ([id])
  GO
ALTER TABLE [dbo].[jhi_review]  WITH CHECK ADD FOREIGN KEY([product_id])
  REFERENCES [dbo].[jhi_product] ([id])
  GO
ALTER TABLE [dbo].[jhi_review]  WITH CHECK ADD FOREIGN KEY([user_id])
  REFERENCES [dbo].[jhi_user] ([id])
  GO
ALTER TABLE [dbo].[jhi_user_authority]  WITH CHECK ADD FOREIGN KEY([authority_name])
  REFERENCES [dbo].[jhi_authority] ([name])
  GO
ALTER TABLE [dbo].[jhi_user_authority]  WITH CHECK ADD FOREIGN KEY([user_id])
  REFERENCES [dbo].[jhi_user] ([id])
  GO
ALTER TABLE [dbo].[jhi_cart_item]  WITH CHECK ADD  CONSTRAINT [CHK_cart_item_quantity_positive] CHECK  (([quantity]>(0)))
  GO
ALTER TABLE [dbo].[jhi_cart_item] CHECK CONSTRAINT [CHK_cart_item_quantity_positive]
  GO
ALTER TABLE [dbo].[jhi_order]  WITH CHECK ADD  CONSTRAINT [CHK_order_status] CHECK  (([status]=N'CANCELLED' OR [status]=N'ĐÃ HOÀN THÀNH' OR [status]=N'HOÀN THÀNH' OR [status]=N'COMPLETED' OR [status]=N'DELIVERED' OR [status]=N'SHIPPED' OR [status]=N'PROCESSING' OR [status]=N'PENDING'))
  GO
ALTER TABLE [dbo].[jhi_order] CHECK CONSTRAINT [CHK_order_status]
  GO
ALTER TABLE [dbo].[jhi_order_item]  WITH CHECK ADD  CONSTRAINT [CHK_order_item_price_positive] CHECK  (([price]>(0)))
  GO
ALTER TABLE [dbo].[jhi_order_item] CHECK CONSTRAINT [CHK_order_item_price_positive]
  GO
ALTER TABLE [dbo].[jhi_order_item]  WITH CHECK ADD  CONSTRAINT [CHK_order_item_quantity_positive] CHECK  (([quantity]>(0)))
  GO
ALTER TABLE [dbo].[jhi_order_item] CHECK CONSTRAINT [CHK_order_item_quantity_positive]
  GO
ALTER TABLE [dbo].[jhi_payment]  WITH CHECK ADD  CONSTRAINT [CHK_payment_amount_positive] CHECK  (([amount] IS NULL OR [amount]>(0)))
  GO
ALTER TABLE [dbo].[jhi_payment] CHECK CONSTRAINT [CHK_payment_amount_positive]
  GO
ALTER TABLE [dbo].[jhi_product]  WITH CHECK ADD  CONSTRAINT [CHK_product_price_positive] CHECK  (([price]>(0)))
  GO
ALTER TABLE [dbo].[jhi_product] CHECK CONSTRAINT [CHK_product_price_positive]
  GO
ALTER TABLE [dbo].[jhi_product]  WITH CHECK ADD  CONSTRAINT [CHK_product_quantity_nonnegative] CHECK  (([quantity]>=(0)))
  GO
ALTER TABLE [dbo].[jhi_product] CHECK CONSTRAINT [CHK_product_quantity_nonnegative]
  GO
ALTER TABLE [dbo].[jhi_review]  WITH CHECK ADD  CONSTRAINT [CHK_review_rating_range] CHECK  (([rating]>=(1) AND [rating]<=(5)))
  GO
ALTER TABLE [dbo].[jhi_review] CHECK CONSTRAINT [CHK_review_rating_range]
  GO

USE [analytics_db]
GO
/****** Object:  User [jhipster_user]    Script Date: 12/19/2025 2:42:04 PM ******/
CREATE USER [jhipster_user] FOR LOGIN [jhipster_user] WITH DEFAULT_SCHEMA=[dbo]
GO
ALTER ROLE [db_owner] ADD MEMBER [jhipster_user]
GO
/****** Object:  Table [dbo].[analytics_log]    Script Date: 12/19/2025 2:42:04 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[analytics_log](
  [id] [bigint] IDENTITY(1,1) NOT NULL,
  [action] [nvarchar](100) NOT NULL,
  [user_id] [nvarchar](255) NULL,
  [entity_type] [nvarchar](100) NULL,
  [entity_id] [bigint] NULL,
  [details] [nvarchar](1000) NULL,
  [ip_address] [nvarchar](50) NULL,
  [user_agent] [nvarchar](500) NULL,
  [timestamp] [datetime2](7) NOT NULL,
  PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
  ) ON [PRIMARY]
  GO
/****** Object:  Table [dbo].[notification]    Script Date: 12/19/2025 2:42:04 PM ******/
  SET ANSI_NULLS ON
  GO
  SET QUOTED_IDENTIFIER ON
  GO
CREATE TABLE [dbo].[notification](
  [id] [bigint] IDENTITY(1,1) NOT NULL,
  [message] [nvarchar](500) NOT NULL,
  [type] [nvarchar](20) NOT NULL,
  [created_at] [datetime2](7) NOT NULL,
  [user_id] [nvarchar](255) NOT NULL,
  [is_read] [bit] NOT NULL,
  [link] [nvarchar](500) NULL,
  [target_role] [nvarchar](50) NULL,
  PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
  ) ON [PRIMARY]
  GO
/****** Object:  Table [dbo].[support_message]    Script Date: 12/19/2025 2:42:04 PM ******/
  SET ANSI_NULLS ON
  GO
  SET QUOTED_IDENTIFIER ON
  GO
CREATE TABLE [dbo].[support_message](
  [id] [bigint] IDENTITY(1,1) NOT NULL,
  [ticket_id] [bigint] NOT NULL,
  [sender_email] [nvarchar](255) NOT NULL,
  [message] [nvarchar](max) NOT NULL,
  [is_from_admin] [bit] NOT NULL,
  [created_at] [datetimeoffset](6) NOT NULL,
  [is_read] [bit] NOT NULL,
  PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
  ) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
  GO
/****** Object:  Table [dbo].[support_ticket]    Script Date: 12/19/2025 2:42:04 PM ******/
  SET ANSI_NULLS ON
  GO
  SET QUOTED_IDENTIFIER ON
  GO
CREATE TABLE [dbo].[support_ticket](
  [id] [bigint] IDENTITY(1,1) NOT NULL,
  [user_email] [nvarchar](255) NOT NULL,
  [title] [nvarchar](500) NULL,
  [status] [nvarchar](50) NOT NULL,
  [assigned_to] [nvarchar](255) NULL,
  [created_date] [datetimeoffset](6) NOT NULL,
  [last_modified_date] [datetimeoffset](6) NULL,
  [closed_at] [datetimeoffset](6) NULL,
  PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
  ) ON [PRIMARY]
  GO
ALTER TABLE [dbo].[analytics_log] ADD  DEFAULT (getdate()) FOR [timestamp]
  GO
ALTER TABLE [dbo].[notification] ADD  CONSTRAINT [DF_notification_created_at]  DEFAULT (getdate()) FOR [created_at]
  GO
ALTER TABLE [dbo].[notification] ADD  CONSTRAINT [DF_notification_is_read]  DEFAULT ((0)) FOR [is_read]
  GO
ALTER TABLE [dbo].[support_message] ADD  DEFAULT ((0)) FOR [is_from_admin]
  GO
ALTER TABLE [dbo].[support_message] ADD  DEFAULT (sysdatetimeoffset()) FOR [created_at]
  GO
ALTER TABLE [dbo].[support_message] ADD  DEFAULT ((0)) FOR [is_read]
  GO
ALTER TABLE [dbo].[support_ticket] ADD  DEFAULT ('OPEN') FOR [status]
  GO
ALTER TABLE [dbo].[support_ticket] ADD  DEFAULT (sysdatetimeoffset()) FOR [created_date]
  GO
ALTER TABLE [dbo].[support_ticket] ADD  DEFAULT (sysdatetimeoffset()) FOR [last_modified_date]
  GO
ALTER TABLE [dbo].[support_message]  WITH CHECK ADD  CONSTRAINT [FK_support_message_ticket] FOREIGN KEY([ticket_id])
  REFERENCES [dbo].[support_ticket] ([id])
  ON DELETE CASCADE
GO
ALTER TABLE [dbo].[support_message] CHECK CONSTRAINT [FK_support_message_ticket]
  GO
/****** Object:  StoredProcedure [dbo].[sp_CleanupOldClosedTickets]    Script Date: 12/19/2025 2:42:04 PM ******/
  SET ANSI_NULLS ON
  GO
  SET QUOTED_IDENTIFIER ON
  GO
CREATE PROCEDURE [dbo].[sp_CleanupOldClosedTickets]
    @DaysToKeep INT = 90
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @CutoffDate DATETIMEOFFSET;
    SET @CutoffDate = DATEADD(DAY, -@DaysToKeep, SYSDATETIMEOFFSET());

DELETE FROM support_ticket
WHERE status = 'CLOSED' AND closed_at < @CutoffDate;

PRINT 'Old closed tickets cleaned up successfully';
END
GO
/****** Object:  StoredProcedure [dbo].[sp_CleanupOldNotifications]    Script Date: 12/19/2025 2:42:04 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE PROCEDURE [dbo].[sp_CleanupOldNotifications]
    @DaysToKeep INT = 30
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @CutoffDate DATETIME2;
    SET @CutoffDate = DATEADD(DAY, -@DaysToKeep, GETDATE());

DELETE FROM notification
WHERE created_at < @CutoffDate;

PRINT 'Old notifications cleaned up successfully';
END
GO
/****** Object:  StoredProcedure [dbo].[sp_GetActiveTickets]    Script Date: 12/19/2025 2:42:04 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER OFF
GO

CREATE PROCEDURE [dbo].[sp_GetActiveTickets]
AS
BEGIN
    SET NOCOUNT ON;

SELECT
  t.id,
  t.user_email,
  t.title,
  t.status,
  t.assigned_to,
  t.created_date,
  t.last_modified_date,
  COUNT(CASE WHEN m.is_read = 0 AND m.is_from_admin = 0 THEN 1 END) as unread_count
FROM support_ticket t
       LEFT JOIN support_message m ON t.id = m.ticket_id
WHERE t.status IN ('OPEN', 'IN_PROGRESS', 'WAITING_FOR_CUSTOMER')
GROUP BY t.id, t.user_email, t.title, t.status, t.assigned_to, t.created_date, t.last_modified_date
ORDER BY t.created_date DESC;
END

GO
/****** Object:  StoredProcedure [dbo].[sp_GetAnalyticsSummary]    Script Date: 12/19/2025 2:42:04 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

-- 3. Create stored procedure for analytics summary
CREATE   PROCEDURE [dbo].[sp_GetAnalyticsSummary]
    @StartDate DATETIME2 = NULL,
    @EndDate DATETIME2 = NULL
AS
BEGIN
    SET NOCOUNT ON;

    -- Default to last 30 days if no dates provided
    IF @StartDate IS NULL
        SET @StartDate = DATEADD(DAY, -30, GETDATE());

    IF @EndDate IS NULL
        SET @EndDate = GETDATE();

SELECT
  action,
  COUNT(*) as total_count,
  COUNT(DISTINCT user_id) as unique_users,
  MIN(timestamp) as first_occurrence,
  MAX(timestamp) as last_occurrence
FROM analytics_log
WHERE timestamp BETWEEN @StartDate AND @EndDate
GROUP BY action
ORDER BY total_count DESC;
END
GO
/****** Object:  StoredProcedure [dbo].[sp_GetPopularProducts]    Script Date: 12/19/2025 2:42:04 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

-- 5. Create stored procedure for popular products
CREATE   PROCEDURE [dbo].[sp_GetPopularProducts]
    @TopN INT = 10
AS
BEGIN
    SET NOCOUNT ON;

SELECT TOP (@TopN)
                                  entity_id as product_id,
  COUNT(*) as view_count,
       COUNT(DISTINCT user_id) as unique_viewers,
       MAX(timestamp) as last_viewed
FROM analytics_log
WHERE action = 'VIEW_PRODUCT'
  AND entity_type = 'Product'
  AND entity_id IS NOT NULL
GROUP BY entity_id
ORDER BY view_count DESC;
END
GO
/****** Object:  StoredProcedure [dbo].[sp_GetTicketMessages]    Script Date: 12/19/2025 2:42:04 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE PROCEDURE [dbo].[sp_GetTicketMessages]
    @TicketId BIGINT
AS
BEGIN
    SET NOCOUNT ON;

SELECT
  id,
  ticket_id,
  sender_email,
  message,
  is_from_admin,
  created_at,
  is_read
FROM support_message
WHERE ticket_id = @TicketId
ORDER BY created_at ASC;
END
GO
/****** Object:  StoredProcedure [dbo].[sp_GetUserActivityReport]    Script Date: 12/19/2025 2:42:04 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

-- 4. Create stored procedure for user activity report
CREATE   PROCEDURE [dbo].[sp_GetUserActivityReport]
    @UserId NVARCHAR(255)
AS
BEGIN
    SET NOCOUNT ON;

SELECT
  action,
  entity_type,
  entity_id,
  details,
  timestamp
FROM analytics_log
WHERE user_id = @UserId
ORDER BY timestamp DESC;
END
GO
/****** Object:  StoredProcedure [dbo].[sp_GetUserNotifications]    Script Date: 12/19/2025 2:42:04 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE PROCEDURE [dbo].[sp_GetUserNotifications]
    @UserId NVARCHAR(255),
    @OnlyUnread BIT = 0,
    @MaxCount INT = 50
AS
BEGIN
    SET NOCOUNT ON;

    IF @OnlyUnread = 1
BEGIN
SELECT TOP (@MaxCount)
         id, message, type, created_at, user_id, is_read, link
FROM notification
WHERE user_id = @UserId AND is_read = 0
ORDER BY created_at DESC;
END
ELSE
BEGIN
SELECT TOP (@MaxCount)
         id, message, type, created_at, user_id, is_read, link
FROM notification
WHERE user_id = @UserId
ORDER BY created_at DESC;
END
END
GO
/****** Object:  StoredProcedure [dbo].[sp_GetUserTickets]    Script Date: 12/19/2025 2:42:04 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE PROCEDURE [dbo].[sp_GetUserTickets]
    @UserEmail NVARCHAR(255)
AS
BEGIN
    SET NOCOUNT ON;

SELECT
  t.id,
  t.user_email,
  t.title,
  t.status,
  t.assigned_to,
  t.created_date,
  t.last_modified_date,
  t.closed_at,
  COUNT(CASE WHEN m.is_read = 0 AND m.is_from_admin = 1 THEN 1 END) as unread_count
FROM support_ticket t
       LEFT JOIN support_message m ON t.id = m.ticket_id
WHERE t.user_email = @UserEmail
GROUP BY t.id, t.user_email, t.title, t.status, t.assigned_to, t.created_date, t.last_modified_date, t.closed_at
ORDER BY t.created_date DESC;
END
GO

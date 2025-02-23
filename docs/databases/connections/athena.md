---
title: Amazon Athena
---

# Amazon Athena

To add a database connection, click on the **gear** icon in the top right, and navigate to **Admin settings** > **Databases** > **Add a database**.

## Connecting to Athena

To connect Metabase to Athena, you'll need to input your IAM credentials:

- Access key
- Secret Key

Metabase will encrypt these credentials.

If you use other AWS services, we recommend that you create a special AWS Service Account that only has the permissions required to run Athena, and input the IAM credentials from that account to connect Metabase to Athena.

See [Identity and access management in Athena](https://docs.aws.amazon.com/athena/latest/ug/security-iam-athena.html).

## Connecting using AWS Default Credentials Chain

If you're running Metabase on AWS and want to use [AWS Default Credentials Chain](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html#credentials-default), leave the Access and Secret keys blank.

- For EC2, you can use [instance profiles](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_use_switch-role-ec2_instance-profiles.html).
- For ECS, you can use [IAM roles for tasks](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-iam-roles.html)

In both cases, the Athena driver will automatically fetch session credentials based on which IAM role you've configured.

## Settings

You can edit these settings at any time. Just remember to save your changes.

### Display name

The display name for the database in the Metabase interface.

### Region

The AWS region where your database is hosted, for Amazon Athena. For example, you might enter `us-east-1`.

### Workgroup

AWS workgroup. For example: `primary`. See [documentation on workgroups](https://docs.aws.amazon.com/athena/latest/ug/user-created-workgroups.html).

### S3 Staging directory

This S3 staging directory must be in the same region you specify above.

### Access key

Part of IAM credentials for AWS. Metabase will encrypt these credentials.

If you're running Metabase on AWS and want to use [AWS Default Credentials Chain](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html#credentials-default), leave the Access and Secret keys blank.

### Secret Key

Part of IAM credentials for AWS. Metabase will encrypt these credentials.

### Additional Athena connection string options

You can specify additional options via a string, e.g. `UseResultsetStreaming=0;LogLevel=6`.

### Re-run queries for simple explorations

Turn this option **OFF** if people want to click **Run** (the play button) before applying any [Summarize](../../questions/query-builder/introduction.md#grouping-your-metrics) or filter selections.

By default, Metabase will execute a query as soon as you choose an grouping option from the **Summarize** menu or a filter condition from the [drill-through menu](https://www.metabase.com/glossary/action_menu). If your database is slow, you may want to disable re-running to avoid loading data on each click.

### Choose when Metabase syncs and scans

Turn this option **ON** to manage the queries that Metabase uses to stay up to date with your database. For more information, see [Syncing and scanning databases](../connecting.md#syncing-and-scanning-databases).

#### Database syncing

If you've selected **Choose when syncs and scans happen** > **ON**, you'll see the following options under **Database syncing**:

- **Scan** sets the frequency of the [sync query](../connecting.md#how-database-syncs-work) to hourly (default) or daily.
- **at** sets the time when your sync query will run against your database (in the timezone of the server where your Metabase app is running).

#### Scanning for filter values

Metabase can scan the values present in each field in this database to enable checkbox filters in dashboards and questions. This can be a somewhat resource-intensive process, particularly if you have a very large database.

If you've selected **Choose when syncs and scans happen** > **ON**, you'll see the following options under **Scanning for filter values**:

- **Regularly, on a schedule** allows you to run [scan queries](../connecting.md#how-database-scans-work) at a frequency that matches the rate of change to your database. The time is set in the timezone of the server where your Metabase app is running. This is the best option for a small database, or tables with distinct values that get updated often.
- **Only when adding a new filter widget** is a great option if you want scan queries to run on demand. Turning this option **ON** means that Metabase will only scan and cache the values of the field(s) that are used when a new filter is added to a dashboard or SQL question.
- **Never, I'll do this manually if I need to** is an option for databases that are either prohibitively large, or which never really have new values added. Use the [Re-scan field values now](../connecting.md#manually-scanning-column-values) button to run a manual scan and bring your filter values up to date.

### Periodically refingerprint tables

Turn this option **ON** to scan a _sample_ of values every time Metabase runs a [sync](../connecting.md#how-database-syncs-work).

A fingerprinting query examines the first 10,000 rows from each column and uses that data to guesstimate how many unique values each column has, what the minimum and maximum values are for numeric and timestamp columns, and so on. If you turn this option **OFF**, Metabase will only fingerprint your columns once during setup.

### Default result cache duration

{% include plans-blockquote.html feature="Database-specific caching" %}

How long to keep question results. By default, Metabase will use the value you supply on the [cache settings page](../../configuring-metabase/caching.md), but if this database has other factors that influence the freshness of data, it could make sense to set a custom duration. You can also choose custom durations on individual questions or dashboards to help improve performance.

Options are:

- **Use instance default (TTL)**. TTL is time to live, meaning how long the cache remains valid before Metabase should run the query again.
- **Custom**.

If you are on a paid plan, you can also set cache duration per questions. See [Advanced caching controls](../../configuring-metabase/caching.md#advanced-caching-controls).

## Further reading

- [Managing databases](../../databases/connecting.md)
- [Metadata editing](../../data-modeling/metadata-editing.md)
- [Models](../../data-modeling/models.md)
- [Setting data access permissions](../../permissions/data.md)

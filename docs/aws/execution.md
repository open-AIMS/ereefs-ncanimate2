# AWS Execution

The AWS infrastructure is based on *Tasks*. NcAnimate batch jobs expect a *Task ID*
to know what needs to be done.

## Task

First, create a *Task* in the MongoDB collection `task` or find one that has what you need.

To connect to the MongoDB, follow this [MongoDB guide](mongodb.md).

Tasks' JSON documents looks like this:

```
{
    "_id": "NcAnimateTask_b5294561-e6a4-4ff1-89da-9bd2235bd8ef",
    "type": "ncanimate",
    "jobId": "Job_ac99c45f-f18d-4301-911a-d185981320ef",
    "productDefinitionId": "products__ncanimate__ereefs__gbr4_v2__temp-wind-salt-current_hourly",
    "status": "CREATED",
    "history": [
        {
            "timestamp": "2020-04-02T16:59:51.775+10:00",
            "status": "CREATED",
            "description": "Task created."
        }
    ],
    "stage": "operational",
    "executionContext": {},
    "region": "queensland-1",
    "dependsOn": []
}
```

The only attributes required by NcAnimate are:
```
{
    "_id": "NcAnimateTask_b5294561-e6a4-4ff1-89da-9bd2235bd8ef",
    "type": "ncanimate",
    "productDefinitionId": "products__ncanimate__ereefs__gbr4_v2__temp-wind-salt-current_hourly",
    "region": "queensland-1"
}
```

| Attribute             | Type   | Description                                                                 |
| --------------------- | ------ | --------------------------------------------------------------------------- |
| `_id`                 | String | A unique ID                                                                 |
| `type`                | String | Must be `ncanimate` or else NcAnimate will return an error                  |
| `productDefinitionId` | String | The NcAnimate product ID, as found in MongoDB collection `ncanimate_config` |
| `region`              | String | An optional region ID to generate. The region must be listed in the chosen `ncanimate_config`. If this attribute is omitted, NcAnimate will generate all regions defined in the `ncanimate_config`. |

## Batch job

The system only automatically runs tasks it has created.
If you want to run a task you manually created, or you want to manually re-run a task,
you will need to launch a Batch job.

1. Log on AWS: https://console.aws.amazon.com/console/home
2. Navigate to *AWS Batch > Job definitions*
3. Select *ereefs-ncanimate-test* or *ereefs-ncanimate-prod*
4. Click the radio button to the left of the latest revision.
    **NOTE:** Clicking the revision link WON'T WORK! You need to click the *Radio button*.
5. Click the *[Actions]* button, then select *Submit job*
6. Fill the form:
    1. Job run-time
        1. Job name: [NcAnimate_queensland-1_2020-04-07_10h40]
        3. Job queue: [ereefs-management_test]
    2. Environment variables
        1. Leave the *EXECUTION_ENVIRONMENT* variable as-is
        2. Click: *Add environment variable*
        3. Key: `TASK_ID`,
            Value: `NcAnimateTask_b5294561-e6a4-4ff1-89da-9bd2235bd8ef`
            (use the ID of the task that needs to be run)
    3. Click *[Submit job]* button

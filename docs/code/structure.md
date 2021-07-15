# Code structure

## MongoDB

NcAnimate connects to the DB using AWS parameters.

For more information about MongoDB, see the [MongoDB documentation](../aws/mongodb.md).


## Split between `ereefs-ncanimate2` and `ereefs-ncanimate2-frame`

NcAnimate uses the `edal` library (package `uk.ac.rdg.resc`) and the `UCAR` library (package `edu.ucar`)
to extract information from NetCDF / GRIB2 files and to rasterise the data (as a `java.awt.image.BufferedImage`).

Unfortunately, those libraries have significant memory leaks which slows down NcAnimate and can cause OutOfMemory
errors which prevents NcAnimate from generating all its product.

To mitigate this issue, NcAnimate was split into 2 applications:
- `ereefs-ncanimate2`
- `ereefs-ncanimate2-frame`


### Package `ereefs-ncanimate2`

`ereefs-ncanimate2` is the main application. It calls `ereefs-ncanimate2-frame` to generate video frames
or static maps then combine the frames into videos.

It is called by the eReefs system with a task ID, specified as a parameter from the command line,
or as an environment variable.
It then connects to MongoDB using credentials found in the `AWS SSM Parameter Store`.
It loads the task from the MongoDB, then identifies which NcAnimate configuration will be needed to complete the task.
It loads the NcAnimate configuration specified by `productDefinitionId` defined in
the task, and starts the generation of frames and product outputs.


**AWS SSM parameters**

| Parameter                                             | Type    | Necessity | Description |
| ----------------------------------------------------- | ------- | --------- | ----------- |
| `/<EXECUTION_ENVIRONMENT>/global/mongodb/host`        | String  | Mandatory | Database host URL. Example: `ip-10-0-0-1.ap-southeast-2.compute.internal` |
| `/<EXECUTION_ENVIRONMENT>/global/mongodb/port`        | String  | Mandatory | Database port. This parameter is expected to be a String representation of an integer. Example: `27017` |
| `/<EXECUTION_ENVIRONMENT>/global/mongodb/db`          | String  | Mandatory | Name of the MongoDB database to use. Example: `ereefs` |
| `/<EXECUTION_ENVIRONMENT>/ncAnimate/mongodb/userid`   | String  | Mandatory | Database user to use to connect to the database. |
| `/<EXECUTION_ENVIRONMENT>/ncAnimate/mongodb/password` | String  | Mandatory | Database password to use to connect to the database. |

> **NOTE:** `<EXECUTION_ENVIRONMENT>` can either be `test` or `prod`.


**Jar parameters**

| Parameter index       | Used for | Type    | Necessity | Description |
| --------------------- | -------- | ------- | ----------| ----------- |
| 1                     | Task ID  | String  | Optional  | The ID of the eReefs task to execute. |


**Environment variables**

| Variable              | Type   | Necessity | Description |
| --------------------- | ------ | --------- | ----------- |
| `TASK_ID`             | String | Optional  | The ID of the eReefs task to execute. |


**Task fields used by NcAnimate**

| Field                 | Type   | Necessity | Description |
| --------------------- | ------ | --------- | ----------- |
| `_id`                 | String | Mandatory | The task ID. Must match the task ID sent as parameter or found in environment variable. |
| `type`                | String | Mandatory | The type of task. Must be set to `ncanimate`. |
| `region`              | String | Optional  | The ID of the region to generate. If not specified, NcAnimate will generate all products for all regions. |
| `productDefinitionId` | String | Mandatory | The ID of the NcAnimate configuration. |


**Execution steps**

1. Load the Task associated with the Task ID received in parameter
2. Reads the NcAnimate configuration associated with the task
3. Determine which product outputs can be generated, considering the available input files
4. Determine which of those product outputs are missing or outdated
5. Determine all the frames that will need to be generated for the product outputs to generate
6. Group frames in date range to optimise the number of input files to download without using too much disk space
7. Call NcAnimate frame for each of the date range group <sup>1</sup>
8. Generate products; videos and maps, and their metadata
9. Upload products to S3, save metadata to MongoDB

> 1: NcAnimate frame is called as a sub process, using a system call. This creates a new JVM instance
> to run the process. NcAnimate frame is not load as a Java library within NcAnimate.
> This allows NcAnimate to free the memory used by NcAnimate frame, after the generation of each date range group.
> When the NcAnimate frame process terminate, its JVM also terminate, freeing any memory
> leaked by the `edal` library and the `UCAR` library.


### Package `ereefs-ncanimate2-frame`

This package is called by NcAnimate. It is not intended to be run manually.

> **NOTE:** All required parameters and environment variables are set by NcAnimate before calling it.


**Jar parameters**

| Parameter index       | Used for                   | Type    | Necessity | Description |
| --------------------- | -------------------------- | ------- | ----------| ----------- |
| 1                     | NcAnimate configuration ID | String  | Mandatory | The ID of the NcAnimate configuration. |
| 2                     | Date from                  | String  | Mandatory | The date of the beginning of the date range, in *ISO 8601 format*. |
| 3                     | Date to                    | String  | Mandatory | The date of the end of the date range, in *ISO 8601 format*. |

> **NOTE**: Both dates are set to `null` when generating products for aggregation of type `ALL`
> (products set with a `timeIncrement` of `ETERNITY`)


**Environment variables**

| Variable                  | Type   | Necessity | Description |
| ------------------------- | ------ | ----------| ----------- |
| `DATABASE_SERVER_ADDRESS` | String | Optional  | Database host URL. Example: `ip-10-0-0-1.ap-southeast-2.compute.internal` |
| `DATABASE_SERVER_PORT`    | String | Optional  | Database port. This parameter is expected to be a String representation of an integer. Example: `27017` |
| `DATABASE_NAME`           | String | Optional  | Name of the MongoDB database to use. Example: `ereefs` |
| `NCANIMATE_REGION`        | String | Optional  | Region ID. If specified, NcAnimate frame will only generate the frames for that region. |

> **NOTE:** If a database parameter is missing, the system will initialise the connection to the Database using
> the same AWS SSM parameters as `ereefs-ncanimate2`.


**Execution steps**

1. Reads the configuration
2. Download required input files
3. Download pre-rendered frames, if they are not outdated
4. Generate the required frames

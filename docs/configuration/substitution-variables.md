# Substitution Variables

Almost any string in the configuration can be used as a template, to get contextual value such as the frame date, region label, etc.

The examples found in this section are using this simplified NcAnimate configuration:

```
{
    "_id": "temp-wind-salt-current_hourly",
    "version": "2.0",
    "lastModified": "2019-08-08T16:40:00.000+08:00",
    "enabled": true,

    "regions": [
        {
            "id": "qld",
            "label": "Queensland",
            "bbox": {
                "east": 156,
                "north": -7.6,
                "south": -29.4,
                "west": 142.4
            }
        },
        "torres-strait",
        "cape-york",
        "wet-tropics",
        "burdekin",
        "mackay-whitsunday",
        "fitzroy",
        "burnett-mary",
        "brisbane"
    ],

    "targetHeights": [-1.5],

    "canvas": {
        "id": "default-canvas"
    },

    "defaults": {
        "panel": {
            "id": "default-panel",
            "layers": [
                {
                    "id": "ereefs-model_gbr4-v2",
                    "input": {
                        "id": "downloads__ereefs__gbr4_v2",
                        "timeIncrement": {
                            "increment": 1,
                            "unit": "HOUR"
                        },
                        "licence": "CC-BY 4.0",
                        "author": "eReefs CSIRO GBR4 Hydrodynamic Model v2.0"
                    },
                    "type": "NETCDF",
                    "arrowSize": 10
                },
                "world",
                "australia",
                "reefs",
                "coralSea",
                "catchments",
                "GBRMPA_Bounds",
                "rivers",
                "cities"
            ]
        },
        "legend": "bottom-left-legend"
    },

    "panels": [
        {
            "id": "temp",
            "title": {
                "text": [
                    "Temperature ${ctx.targetHeight %.1f}m",
                    "Temperature"
                ]
            },
            "layerOverwrites": {
                "ereefs-model_gbr4-v2": {
                    "targetHeight": "${ctx.targetHeight}",
                    "variable": "ereefs/gbr4_v2/temp"
                }
            }
        },
        {
            "id": "wind",
            "title": { "text": "Wind speed" },
            "layerOverwrites": {
                "ereefs-model_gbr4-v2": {
                    "variable": "ereefs/gbr4_v2/wind"
                }
            }
        }
    ],

    "render": {
        "id": "default-videos",
        "definitionId": "temp-wind-salt-current",
        "videoTimeIncrement": {
            "increment": 1,
            "unit": "MONTH"
        }
    }
}
```

- Variable are placed using the following format:

    `${VARIABLE}`

- Some variable type have options:

    `${VARIABLE OPTIONS}`

    - Type `Date`
    
        Dates can be formatted using `org.joda.time.DateTime.toString(String format)`, with the options used as date format.
        
        Example: `${ctx.frameDateFrom dd-MMM-yyyy}` parse as `01-Sep-2010`
        
        The option doesn't allow space character. To add space to the date format, use underscore.

        Example: `${ctx.frameDateFrom dd-MMM-yyyy_HH:mm}` parse as `01-Sep-2010 01:00`

        See Joda DateTimeFormat for more information: https://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html
        
    - Type `Number` (Float, Double, Integer, etc)

        Numbers can be formatted using `String.format(String format, Object ... args)`, with the options used as format.

        Example: `Temperature ${ctx.targetHeight %.1f}m` with a targetHeight of `-17.75` will parse as `Temperature -17.8m`

- The variable can be placed anywhere in the string, and multiple variables can be used in a string.

    For example, the following string:

    `"Region ID: ${ctx.region.id}, label: ${ctx.region.label}, height: ${ctx.targetHeight}m"`

    will be parsed into something like:

    `"Region ID: qld, label: Queensland, height: -1.5m"`

- If a variable is not found, it's left as-is in the string:

    `"Region ID: ${ctx.region.id}, label: ${ctx.region.somethingIJustMadeUp}"`

    will be parsed into something like:

    `"Region ID: qld, label: ${ctx.region.somethingIJustMadeUp}"`

- Variables can be used in file paths and URI:

    `"s3://bucket/ncanimate/${id}/${ctx.region.id}_${ctx.targetHeight}.png"`

    will be parsed into something like:

    `"s3://bucket/ncanimate/temp-wind-salt-current_hourly/qld_-1.5.png"`


## Variable from config

- Any field from the current NcAnimate configuration can be used as a variable. One exception to this is the configuration ID. The field is named `_id` in the configuration file but is used as `id`.

    Example: `${id}` parse as `temp-wind-salt-current_hourly`

- If the field is part of a JSON object, it can be referred by its path, using `.` as separator.

    Example: `${render.scale}` parse as `1.0`

    Example: `${render.definitionId}` parse as `temp-wind-salt-current`

- If the field is part of a JSON array, it can be referred by its index or its ID, using square brackets `[` and `]`.

    Example: `${targetHeights[0]}` parse as `-1.5`

> **NOTE:** NcAnimate will go through all regions / targetHeights to generate all combinations of products.
> If you want to access the current targetHeight of fields of the current region, use the special variable `ctx`.

> **Example**
>
> `Region: ${ctx.region.label}, height: ${ctx.targetHeight}m`
>
> parse as
>
> `Region: Queensland, height: -1.5m`


## Configuration field `defaults`

The system remove the field `defaults` after applying the default values to every panels. Therefore, it's not accessible using variables.

To access an attribute of a given panel, refer to it using the `panels` array field.

> **Example**
>
> `${panels[wind].title.text[0]}`
>
> parse as
>
> `Wind speed`

Or, if the substitution variable is inside the configuration of a panel, you can use directly:

> **Example**
>
> `${ctx.panel.config.title.text[0]}`
>
> also parse as
>
> `Wind speed`

## Configuration field `regions`

The system resolved all used regions into a map to make it easy to access a region from its ID. If you need to get an attribute from an arbitrary region, you can refer to it like so:

Example: `${regions.qld.label}` parse as `Queensland`

> **NOTE:** It's more likely that what you need is to access the current region. It can be accessed using the special variable `ctx`.

> **Example**
>
> `Region: ${ctx.region.label}`
>
> parse as
>
> `Region: Queensland`


## Special variable `layers`

In some cases, it's useful to access the configuration value of a layer, without referring to a given panel.
The system give access to a list of all layers, before they get modified by panels overwrites.

The array of layers can be accessed using the special variable `layers`.

> **Example**
>
> `${layers[ereefs-model_gbr4-v2].input.licence}`
>
> parse as
>
> `CC-BY 4.0`


## Special variable `layerCtx`

The special variable `layerCtx` contains the contextual values for each layer, such as `targetHeight`, `frameDate`, etc.
Those values may changed for each frame. They can be access using: `${layerCtx.<LAYER ID>.<FIELD>}`

| Field                   | Type                               | Description |
| ----------------------- | ---------------------------------- | ----------- |
| `layerId`               | String                             | The layer ID. |
| `targetHeight`          | Double                             | Closest target height to the current target height, as found in current input file. |
| `frameDate`             | Date                               | Date used to extract data for the current frame, from the current input file. |
| `inputFile`             | String                             | Metadata ID of the input file used to generate the layer for the current frame. This is short for `${layerCtx.<LAYER ID>.inputFileMetadata.id}`. |
| `inputFileMetadata`     | Object                             | Metadata of the input file used to generate the layer for the current frame. |

> **Examples**
> 
> To access the metadata attribute `paramhead` of the current NetCDF file for the layer `ereefs-model_gbr4-v2`:
> 
> ```
> ${layerCtx.ereefs-model_gbr4-v2.inputFileMetadata.attributes.paramhead}
> ```
> 
> To access the metadata attribute `long_name` of the current NetCDF file for the layer `ereefs-model_gbr4-v2`, for NetCDF variable ID `temp`:
> 
> ```
> ${layerCtx.ereefs-model_gbr4-v2.inputFileMetadata.variables.temp.attributes.long_name}
> ```

## Special variable `ctx`

The variable `ctx` refer to the current generation context. It contains attributes such as the current frame date,
current region, current target height, etc.

| Field                   | Type                               | Description |
| ----------------------- | ---------------------------------- | ----------- |
| `workingDirectory`      | String                             | Path to the local working directory. |
| `renderFile`            | Object (RenderFile)                | The file object, currently been rendered. Only available in the `video` and `map` objects of the `render` section of the config. |
| `frameDirectory`        | String                             | Path to the local directory used to generate frame files. |
| `frameFilenamePrefix`   | String                             | When generating videos, `ffmpeg` expect all frames to be sequentially numbered. This field output the frame filename, without the `_NUMBER` and the `.png` extension. It is expected to be used with: `ffmpeg -i "${ctx.videoFrameDirectory}/${ctx.frameFilenamePrefix}_%05d.png" ...` |
| `videoFrameDirectory`   | String                             | Path to the local directory containing all the video frames, numbered from 0 up to 99999. The number files are symbolic links to the actual frame files. |
| `outputDirectory`       | String                             | Path to the local directory used to generate the output product (map or video) before been uploaded to `directoryUri`. |
| `outputFilenamePrefix`  | String                             | Local product filename, before adding the date, region, targetHeight and extension. Its value is either `map` or `video`. |
| `outputFilename`        | String                             | Local product filename, after adding the date, region, targetHeight and extension. |
| `outputFile`            | String                             | Path to the local product file. |
| `dateRange`             | Object (DateRange)                 | Date range of the generated product. |
| `dateFrom`              | Date                               | The start date of the generated product. This is short for `${ctx.dateRange.startDate}` |
| `dateTo`                | Date                               | The end date of the generated product. This is short for `${ctx.dateRange.endDate}` |
| `region`                | Object (Region)                    | The current region. |
| `targetHeight`          | Float                              | The current target height, aka the inverse of the elevation. |
| `frameTimeIncrement`    | Object (TimeIncrement)             | The time increment between each frame. |
| `framePeriod`           | String                             | Display label for the output file time increment. Example: `Monthly` |
| `netCDFDirectory`       | String                             | Path to the local directory where the NetCDF / GRIB2 files are downloaded. |
| `layerDirectory`        | String                             | Path to the local directory where layer files are downloaded. |
| `paletteDirectory`      | String                             | Path to the local directory where edal colour palette files `.pal` are downloaded. |
| `styleDirectory`        | String                             | Path to the local directory where layer style files `.sld` are downloaded. |
| `canvasWidth`           | Integer                            | Size of the rendered frame images, after scaling. |
| `canvasHeight`          | Integer                            | Size of the rendered frame images, after scaling. |
| `generationDate`        | Date                               | Current date. |
| `maxWidth`              | Integer                            | Maximum expected size of the rendered frame images, after scaling. |
| `maxHeight`             | Integer                            | Maximum expected size of the rendered frame images, after scaling. |
| `padding`               | Object (Padding)                   | The padding that needs to be added to `canvasWidth` x `canvasHeight` to get the final dimensions `productWidth` x `productHeight`. |
| `productWidth`          | Integer                            | Final dimensions of the image, after scaling and padding added. It respect the blockSize, if specified. |
| `productHeight`         | Integer                            | Final dimensions of the image, after scaling and padding added. It respect the blockSize, if specified. |
| `frameFiles`            | Map<Enum, Object>                  | Object representing a frame file, for each used map format (`SVG`, `PNG`, `GIF`, `JPG`). |
| `frameDateRange`        | Object (DateRange)                 | Date range of the current frame. |
| `frameDateFrom`         | Date                               | The start date of the current frame. This is short for `${ctx.frameDateRange.startDate}` |
| `frameDateTo`           | Date                               | The end date of the current frame. This is short for `${ctx.frameDateRange.endDate}` |
| `panel`                 | Object (Panel)                     | Refer to the current panel been rendered. Can only be used in panel configuration. |
| `variableIds`           | String                             | A coma separated string of all NetCDF variable IDs use on the frame. |


**Object Video RenderFile**

| Field                   | Type                               | Description |
| ----------------------- | ---------------------------------- | ----------- |
| `fileType`              | String                             | Type of render file: `VIDEO` |
| `fileURI`               | String                             | URI where the output file will be uploaded after generation, before parsing variables. |
| `maxWidth`              | Integer                            | Maximum expected size of the rendered frame images, before scaling, as defined in the config. |
| `maxHeight`             | Integer                            | Maximum expected size of the rendered frame images, before scaling, as defined in the config. |
| `format`                | String                             | Video file type: `WMV`, `MP4`, `ZIP` |
| `blockSize`             | Array of Integer                   | Block size, as specified in config. |
| `fps`                   | Integer                            | Video frame per seconds (FPS). |


**Object Map RenderFile**

| Field                   | Type                               | Description |
| ----------------------- | ---------------------------------- | ----------- |
| `fileType`              | String                             | Type of render file: `MAP` |
| `fileURI`               | String                             | URI where the output file will be uploaded after generation, before parsing variables. |
| `maxWidth`              | Integer                            | Maximum expected size of the rendered frame images, before scaling, as defined in the config. |
| `maxHeight`             | Integer                            | Maximum expected size of the rendered frame images, before scaling, as defined in the config. |
| `format`                | String                             | Map file type: `SVG`, `PNG`, `GIF`, `JPG` |


**Object DateRange**

| Field                   | Type                               | Description |
| ----------------------- | ---------------------------------- | ----------- |
| `startDate`             | Date                               | The date of the start of the date range. |
| `endDate`               | Date                               | The date of the end of the date range. |


**Object Region**

| Field                   | Type                               | Description |
| ----------------------- | ---------------------------------- | ----------- |
| `id`                    | String                             | Unique region ID. Example: `qld` |
| `label`                 | String                             | Display label of the region. Example: `Queensland` |
| `bbox`                  | Object (Bbox)                      | Region bounding box, in degree. |


**Object Bbox**

| Field                   | Type                               | Description |
| ----------------------- | ---------------------------------- | ----------- |
| `east`                  | Double                             | East longitude, in degree. |
| `west`                  | Double                             | West longitude, in degree. |
| `north`                 | Double                             | North latitude, in degree. |
| `south`                 | Double                             | South latitude, in degree. |


**Object TimeIncrement**

| Field                   | Type                               | Description |
| ----------------------- | ---------------------------------- | ----------- |
| `unit`                  | String                             | `ETERNITY`, `YEAR`, `MONTH`, `WEEK`, `DAY`, `HOUR`, `MINUTE`, `SECOND` |
| `increment`             | Integer                            | Number of `unit` per increment. |


**Object Padding**

| Field                   | Type                               | Description |
| ----------------------- | ---------------------------------- | ----------- |
| `top`                   | Integer                            | Top padding, in pixel. |
| `bottom`                | Integer                            | Bottom padding, in pixel. |
| `left`                  | Integer                            | Left padding, in pixel. |
| `right`                 | Integer                            | Right padding, in pixel. |


**Object Panel**

| Field                   | Type                                  | Description |
| ----------------------- | ------------------------------------- | ----------- |
| `config`                | Object (Panel config)                 | The configuration of the panel, before substitution. See [Datatype PANEL](structure.md#Datatype-PANEL) for list of attribute. |
| `variables`             | Map<String, Object (Variable config)> | A map of all the NetCDF variables used in the panel, indexed by NetCDF variable ID. The value of each entry in the map in the configuration of the NetCDF variable, before substitution. See [Datatype VARIABLE](structure.md#Datatype-VARIABLE) for list of attribute. |
| `variableIds`           | String                                | A coma separated string of all NetCDF variable IDs use on the panel. |

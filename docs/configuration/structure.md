# NcAnimate configuration

The NcAnimate configuration is implemented in a way that facilitate reuse of configuration, reduce duplication
and enforce uniformity of generated products.

It consists of a main configuration and multiple configuration parts.
The main configuration can refer to configuration parts using their ID.
Some configuration parts can also refer to other configuration parts.

The configurations and configuration parts are written in JSON.

> **Example**
>
> ```
> {
>     "_id": "products__ncanimate__ereefs__gbr4_v2__temp-wind-salt-current_hourly",
>     "version": "2.0",
>     "lastModified": "2019-08-08T16:40:00.000+08:00",
>     "enabled": true,
> 
>     "regions": [ "qld" ],
> 
>     "targetHeights": [-1.5],
> 
>     "canvas": {
>         "id": "default-canvas"
>     },
> 
>     ...
> }
> ```


## Main configuration

This documentation is for NcAnimate configuration schema Version 2.0

| Field                 | Type                                  | Necessity                  | Description |
| --------------------- | ------------------------------------- | -------------------------- | ----------- |
| `_id`                 | String                                | Mandatory                  | Unique ID for the configuration. This is the only field starting with a "_", since those fields are reserved by MongoDB |
| `version`             | String                                | Optional. Default: `2.0`   | Version of the configuration schema. This documentation describe the version 2.0 of the configuration schema. |
| `lastModified`        | Date (String / long)                  | Mandatory                  | The date of the last modification of the configuration file. Timestamps (type long) can be used, but are not recommended because they are not user-friendly. It's recommended to use a date string in *ISO 8601 format*. Example: `2019-08-15T12:25:00.000+10:00` |
| `enabled`             | boolean                               | Optional. Default: `false` | If set to `false`, the eReefs system will ignore the configuration. This is used to temporarily pause the generation of a product. |
| `regions`             | Array of [`REGION`](#Datatype-REGION) | Mandatory                  | List the regions this configuration should produce. See [`REGION`](#Datatype-REGION) |
| `targetHeights`       | Array of Double                       | Optional                   | List the heights (aka depth) of the data. |
| `canvas`              | [`CANVAS`](#Datatype-CANVAS)          | Optional                   | Define common elements of all frames; background-colour, text labels, etc. See [`CANVAS`](#Datatype-CANVAS) |
| `defaults`            | Object                                | Optional                   | Define default values for all panels; list of layers, definition of the legend. See sub-section `defaults`. |
| `panels`              | Array of [`PANEL`](#Datatype-PANEL)   | Mandatory                  | Define all panels to be render in frames. |
| `render`              | [`RENDER`](#Datatype-RENDER)          | Mandatory                  | Define the products to generate; videos and/or maps. |


**Field `defaults`**

| Field                 | Type                         | Necessity                | Description |
| --------------------- | ---------------------------- | ------------------------ | ----------- |
| `panel`               | [`PANEL`](#Datatype-PANEL)   | Optional                 | Define the default values for all panels |
| `legend`              | [`LEGEND`](#Datatype-LEGEND) | Optional                 | Define the default values for all legends, rendered in panels. |


> **Example**
> 
> ```
> {
>     "_id": "products__ncanimate__ereefs__gbr4_v2__temp-wind-salt-current_hourly",
>     "version": "2.0",
>     "lastModified": "2019-08-08T16:40:00.000+08:00",
>     "enabled": true,
> 
>     "regions": [
>         "qld",
>         "torres-strait",
>         "cape-york",
>         "wet-tropics",
>         "burdekin",
>         "mackay-whitsunday",
>         "fitzroy",
>         "burnett-mary",
>         "brisbane"
>     ],
> 
>     "targetHeights": [-1.5],
> 
>     "canvas": {
>         "id": "default-canvas"
>     },
> 
>     "defaults": {
>         "panel": {
>             "id": "default-panel",
>             "layers": [
>                 "ereefs-model_gbr4-v2",
>                 "world",
>                 "australia",
>                 "reefs",
>                 "coralSea",
>                 "catchments",
>                 "GBRMPA_Bounds",
>                 "rivers",
>                 "cities"
>             ]
>         },
>         "legend": "bottom-left-legend"
>     },
> 
>     "panels": [
>         {
>             "id": "temp",
>             "title": { "text": "Temperature ${ctx.targetHeight %.1f}m" },
>             "layerOverwrites": {
>                 "ereefs-model_gbr4-v2": {
>                     "targetHeight": "${ctx.targetHeight}",
>                     "variable": "ereefs/gbr4_v2/temp"
>                 }
>             }
>         },
>         {
>             "id": "wind",
>             "title": { "text": "Wind speed" },
>             "layerOverwrites": {
>                 "ereefs-model_gbr4-v2": {
>                     "variable": "ereefs/gbr4_v2/wind"
>                 }
>             }
>         },
>         {
>             "id": "salt",
>             "title": {"text": "Salinity ${ctx.targetHeight %.1f}m"},
>             "layerOverwrites": {
>                 "ereefs-model_gbr4-v2": {
>                     "targetHeight": "${ctx.targetHeight}",
>                     "variable": "ereefs/gbr4_v2/salt"
>                 }
>             }
>         },
>         {
>             "id": "current",
>             "title": {"text": "Current ${ctx.targetHeight %.1f}m"},
>             "layerOverwrites": {
>                 "ereefs-model_gbr4-v2": {
>                     "targetHeight": "${ctx.targetHeight}",
>                     "variable": "ereefs/gbr4_v2/current"
>                 }
>             }
>         }
>     ],
> 
>     "render": {
>         "id": "default-videos",
>         "definitionId": "products__ncanimate__ereefs__gbr4_v2__temp-wind-salt-current",
>         "videoTimeIncrement": {
>             "increment": 1,
>             "unit": "MONTH"
>         }
>     }
> }
> ```

## Configuration parts

Config parts can be defined inline:
```
"variable": {
    "variableId": "alk",
    "legend": {
        "title": { "text": "Total alkalinity (mmol m-3)" }
    },
    "colourPaletteName": "x-Rainbow-inv",
    "scaleMin": 2155,
    "scaleMax": 2281
}
```

or they can refer to a part ID:
```
"variable": "alk"
```

or they can use a part as a base, referred by its ID, and overwrite / define fields:
```
"variable": {
    "id": "alk",
    "colourPaletteName": "x-Rainbow"    
}
```

> **NOTE:** The datatype do not need to be specified when referencing a config part. It's implied by the context.

Each config parts needs to have the following attributes:

| Field                 | Type                               | Necessity                | Description |
| --------------------- | ---------------------------------- | ------------------------ | ----------- |
| `_id`                 | Object                             | Mandatory                | An ID Object unique to the configuration part. See `_id` sub-section. |
| `lastModified`        | Date (String / long)               | Mandatory                | The date of the last modification of the configuration file. Timestamps (type long) can be used, but are not recommended because they are not user-friendly. Example: "2019-08-15T12:25:00.000+10:00" |


**Field `_id`**

The `_id` field is an object. Both fields are necessary to ensure the uniqueness of the configuration part ID.
The split between `id` and `datatype` allows the system to find which configuration part
it needs to load, by supplying the `id` value only in the main configuration,
assuming the `datatype` from the context in which the configuration part is used.

| Field                 | Type                               | Necessity                | Description |
| --------------------- | ---------------------------------- | ------------------------ | ----------- |
| `id`                  | String                             | Mandatory                | An ID unique to the configuration part, within its datatype. |
| `datatype`            | Enum                               | Mandatory                | The configuration datatype. Example: `CANVAS`, `PANEL`, etc. See bellow for a list of configuration datatype. |


> **Example**
> 
> ```
> {
>     "_id": {
>         "id": "default",
>         "datatype": "CANVAS"
>     },
>     "lastModified": "2019-08-15T12:25:00.000+08:00",
> 
>     ...
> }
> ```


### Datatype CANVAS

The `CANVAS` element defines the look and feel of each frame.

| Field                  | Type                                | Necessity                | Description |
| ---------------------- | ----------------------------------- | ------------------------ | ----------- |
| `backgroundColour`     | String                              | Optional                 | The colour used for the background of frames. |
| `padding`              | [`PADDING`](#PADDING)               | Optional                 | Extra space around the frame. |
| `paddingBetweenPanels` | Integer                             | Optional                 | Space between each panel. |
| `texts`                | Object of <String, [`TEXT`](#TEXT)> | Optional                 | Collection of text that can be rendered anywhere on the frame, even on top of panels. |

> **NOTE:** The collection of `TEXT` is a JSON object. This is used to identify each TEXT elements with an ID,
> which can be referred later to overwrite its fields.


> **Example**
> 
> ```
> {
>     "_id": {
>         "id": "default",
>         "datatype": "CANVAS"
>     },
>     "lastModified": "2019-08-15T12:25:00.000+08:00",
> 
>     "backgroundColour": "#FFFFFF",
>     "padding": {
>         "top": 80,
>         "left": 16,
>         "bottom": 49,
>         "right": 16
>     },
>     "paddingBetweenPanels": 16,
>     "texts": {
>         "region": {
>             "fontSize": 25,
>             "bold": false,
>             "text": "${ctx.region.label}",
>             "position": {
>                 "top": 28,
>                 "left": 16
>             },
>             "italic": false
>         },
>         "frameDate": {
>             "fontSize": 25,
>             "bold": true,
>             "text": "${ctx.frameDateFrom dd-MMM-yyyy} ${ctx.frameDateFrom HH:mm}",
>             "position": {"top": 30},
>             "italic": false
>         },
>         "framePeriod": {
>             "fontSize": 25,
>             "bold": false,
>             "text": "${ctx.framePeriod}",
>             "position": {
>                 "top": 28,
>                 "right": 16
>             },
>             "italic": false
>         },
>         "authors": {
>             "fontSize": 12,
>             "bold": false,
>             "text": "Data: ${layers.authors}. Map generation: AIMS",
>             "position": {
>                 "bottom": 28,
>                 "right": 16
>             },
>             "italic": false
>         },
>         "licence": {
>             "fontSize": 12,
>             "bold": false,
>             "text": "Licensing: ${layers.licences}",
>             "position": {
>                 "bottom": 10,
>                 "right": 16
>             },
>             "italic": false
>         }
>     }
> }
> ```


### Datatype PANEL

| Field                  | Type                                           | Necessity                    | Description |
| ---------------------- | ---------------------------------------------- | ---------------------------- | ----------- |
| `title`                | [`TEXT`](#TEXT)                                | Optional                     | Text display above the panel. The text position is relative to the panel top. Default font size is 30. |
| `layers`               | Array of [`LAYER`](#Datatype-LAYER)            | Optional                     | Define which layer are use in the panel, and their order. |
| `layerOverwrites`      | Object of <String, [`LAYER`](#Datatype-LAYER)> | Optional                     | Used to overwrite fields of [`LAYER`](#Datatype-LAYER) defined in the `layers` field. |
| `width`                | Integer                                        | Mandatory                    | Width of the panel, in pixels. The height is calculated proportionally with the current region dimensions. |
| `margin`               | [`PADDING`](#PADDING)                          | Optional. Default: `{ "top": 0, "bottom": 0, "left": 0, "right": 0 }` | The margin around the panel. |
| `borderWidth`          | Integer                                        | Optional. Default: `2`       | Width of the panel border, in pixels. |
| `borderColour`         | String                                         | Optional. Default: `#000000` | Colour used for the panel's border. |
| `backgroundColour`     | String                                         | Optional. Default: `#FFFFFF` | Colour used for the panel's background. |
| `mapScale`             | Integer                                        | Deprecated                   | Was used to specify the scale for the layers' SLD style, for the layer. If we still want to use this field, it should be moved to [`REGION`](#Datatype-REGION). |
| `description`          | String                                         | Unused                       | Can be used to add comments, helpful for the maintenance of the configuration. |
| `texts`                | Object of <String, [`TEXT`](#TEXT)>            | Optional                     | Collection of text that can be rendered anywhere on the frame, with position relative to the panel. |

> **NOTE:** The field `layers` is usually defined in defaults, to ensure some uniformity between panels and avoid some
> configuration duplication. Layers can be individually overwritten using the `layerOverwrites` field in each specific panel.
> The `layerOverwrites` field is used in each panel to specify which variable to display with each NetCDF / GRIB2 layer.


> **Example**
> 
> ```
> {
>     "_id": {
>         "id": "default",
>         "datatype": "PANEL"
>     },
>     "lastModified": "2019-08-15T12:25:00.000+08:00",
> 
>     "borderColour": "#666666",
>     "borderWidth": 2,
>     "width": 452,
>     "backgroundColour": "#F4FDFD",
>     "title": {
>         "position": {
>             "top": 10
>         },
>         "fontSize": 30,
>         "fontColour": "#000000"
>     },
>     "texts": {
>         "panelVariableIds": {
>             "fontSize": 10,
>             "text": "Variable ID: ${ctx.panel.variableIds}",
>             "fontColour": "#666666",
>             "position": {
>                 "bottom": -16,
>                 "right": 0
>             }
>         }
>     }
> }
> ```


### Datatype LEGEND

| Field                   | Type                               | Necessity                    | Description |
| --------------------------- | ---------------------------------- | ---------------------------- | ----------- |
| `title`                     | [`TEXT`](#TEXT)                    | Optional                     | Configuration of the text display to the right of the legend. |
| `label`                     | [`TEXT`](#TEXT)                    | Optional                     | Configuration of the scale labels display on the legend.<br>**NOTE**: `position` is ignored with `label`. |
| `step`                      | Integer                            | Optional                     | The number of labels to display in the legend. |
| `labelPrecision`            | Integer                            | Optional                     | The number of digits to display for the numbers in the legend. |
| `labelMultiplier`           | Float                              | Optional                     | Each values in the legend are multiplied by this number. Can be used to convert units, or generate prettier legend with small values or large values. Remember to also alter the legend title to describe the new unit. |
| `labelOffset`               | Float                              | Optional                     | Each values in the legend are offset by this number. Can be used to convert units. Note that `labelMultiplier` is applied before `labelOffset`. |
| `position`                  | [`POSITION`](#POSITION)            | Optional                     | Position of the legend, relative to the panel it's in. |
| `padding`                   | [`PADDING`](#PADDING)              | Optional. Default: `{ "top": 5, "bottom": 5, "left": 5, "right": 5 }` | Space around the legend, filled with the background colour. |
| `backgroundColour`          | String                             | Optional. Default: `#FFFFFF` | Colour used for the background of the legend. |
| `colourBandWidth`           | Integer                            | Optional. Default: `20`      | Width of the colour band displayed in the legend. |
| `colourBandHeight`          | Integer                            | Optional. Default: `300`     | Height of the colour band displayed in the legend. |
| `colourBandColourCount`     | Integer                            | Optional. Default: `250`     | Number of colours used to generate the layer and the legend. |
| `majorTickMarkLength`       | Integer                            | Optional. Default: `6`       | Length of the big tick marks in the legend. The ones next to numbers. |
| `minorTickMarkLength`       | Integer                            | Optional. Default: `3`       | Length of the small tick marks in the legend. The ones between numbers.<br>Disabled with colourSchemeType thresholds and coloured arrows. It has no effect when the variable `colourSchemeType` is set to `thresholds` or `arrow_thresholds`. |
| `hideLowerLabel`            | Boolean                            | Optional. Default: `false`   | Prevent rendering the smaller number in the legend, and its tick mark. It has no effect when the variable `colourSchemeType` is set to `thresholds` or `arrow_thresholds`. |
| `hideHigherLabel`           | Boolean                            | Optional. Default: `false`   | Prevent rendering the larger number in the legend, and its tick mark. It has no effect when the variable `colourSchemeType` is set to `thresholds` or `arrow_thresholds`. |
| `extraAmountOutOfRangeLow`  | Float                              | Optional. Default: `0.1`     | Ratio of the colour band used to represent the colour for the lowest value. Act as padding in the colour band. It has no effect when the variable `colourSchemeType` is set to `thresholds` or `arrow_thresholds`. |
| `extraAmountOutOfRangeHigh` | Float                              | Optional. Default: `0.1`     | Ratio of the colour band used to represent the colour for the highest value. Act as padding in the colour band. It has no effect when the variable `colourSchemeType` is set to `thresholds` or `arrow_thresholds`. |

> **Example**
> 
> ```
> {
>     "_id": {
>         "id": "default",
>         "datatype": "LEGEND"
>     },
>     "lastModified": "2019-08-15T12:25:00.000+08:00",
> 
>     "colourBandWidth": 20,
>     "colourBandHeight": 300,
>     "colourBandColourCount": 250,
>     "position": {
>         "bottom": 5,
>         "left": 5
>     },
>     "title": {
>         "fontColour": "#000000",
>         "fontSize": 16,
>         "bold": true,
>         "position": {
>             "top": 10
>         }
>     },
>     "label": {
>         "fontSize": 14,
>         "position": {
>             "left": 5,
>             "bottom": 5
>         }
>     }
> }
> ```


### Datatype LAYER

| Field                  | Type                               | Necessity                    | Description |
| ---------------------- | ---------------------------------- | ---------------------------- | ----------- |
| `type`                 | Enum                               | Optional                     | Type of layer. Supported values are: [`NETCDF`](#NETCDF-and-GRIB2-layer), [`GRIB2`](#NETCDF-and-GRIB2-layer), [`GEOJSON`](#GEOJSON-layer), [`CSV`](#CSV-layer), [`WMS`](#WMS-layer). |


#### GEOJSON layer

| Field                  | Type                               | Necessity                    | Description |
| ---------------------- | ---------------------------------- | ---------------------------- | ----------- |
| `datasource`           | String (URI)                       | Mandatory                    | The URI of the GeoJSON file. Supports S3 URI and local file URI. Example: "s3://bucket/layers/australia.geojson" |
| `style`                | String (URI)                       | Mandatory                    | The URI of the SLD style file. Supports S3 URI and local file URI. Example: "s3://bucket/styles/australia.sld" |

> **Example**
> 
> ```
> {
>     "_id": {
>         "id": "australia",
>         "datatype": "LAYER"
>     },
>     "lastModified": "2019-08-15T12:25:00.000+08:00",
> 
>     "datasource": "/tmp/ncanimateTests/s3/layers/AU_GA_GEODATA-TOPO-5M_aus5fgd_r.geojson",
>     "style": "/tmp/ncanimateTests/s3/styles/AU_GA_GEODATA-TOPO-5M_aus5fgd_r.sld",
>     "type": "GEOJSON"
> }
> ```

#### CSV layer

NcAnimate can create a layer from a CSV file. The file must define a series of point. Polygons and lines are only supported by GeoJSON layers.

| Field                  | Type                               | Necessity                    | Description |
| ---------------------- | ---------------------------------- | ---------------------------- | ----------- |
| `datasource`           | String (URI)                       | Mandatory                    | The URI of the CSV file. Supports S3 URI and local file URI. Example: "s3://bucket/layers/cities.csv" |
| `style`                | String (URI)                       | Mandatory                    | The URI of the SLD style file. Supports S3 URI and local file URI. Example: "s3://bucket/styles/cities.sld" |
| `latitudeColumn`       | String                             | Mandatory                    | Name of the CSV column containing latitude coordinates. |
| `longitudeColumn`      | String                             | Mandatory                    | Name of the CSV column containing longitude coordinates. |

> **CSV file example**
> 
>     SCALERANK, NATSCALE, LABELRANK, LATITUDE, LONGITUDE, NAME
>     10,        1,        8,         -15.469,  145.2515,  Cooktown
>     10,        1,        8,         -34.475,  -57.84,    Colonia del Sacramento
>     ...

> **NOTE:** The SLD file is responsible for styling the point and adding the label.

> **Example**
> 
> ```
> {
>     "_id": {
>         "id": "cities",
>         "datatype": "LAYER"
>     },
>     "lastModified": "2019-08-15T12:25:00.000+08:00",
> 
>     "datasource": "/tmp/ncanimateTests/s3/layers/World_NE_10m-cities_V3_Ranked_NRM.csv",
>     "style": "/tmp/ncanimateTests/s3/styles/World_NE_10m-cities_V3_Ranked_qld.sld",
>     "type": "CSV",
>     "latitudeColumn": "LATITUDE",
>     "longitudeColumn": "LONGITUDE"
> }
> ```

#### WMS layer

| Field                  | Type                               | Necessity                    | Description |
| ---------------------- | ---------------------------------- | ---------------------------- | ----------- |
| `server`               | String (URL)                       | Mandatory                    | The URL of the WMS server. Example: "https://maps.eatlas.org.au/maps/ows" |
| `layerName`            | String                             | Mandatory                    | The name of the WMS layer. Example: "ea-landsat:QLD_e-Atlas-NASA_Landsat_L5096070_07020080501" |
| `styleName`            | String                             | Optional                     | The name of the WMS style. Example: "Landsat-Marine-Blue-Enhance" |

> **Example**
> 
> ```
> {
>     "_id": {
>         "id": "wms",
>         "datatype": "LAYER"
>     },
>     "lastModified": "2019-08-15T12:25:00.000+08:00",
> 
>     "type": "WMS",
>     "server": "https://maps.eatlas.org.au/maps/ows",
>     "layerName": "ea-be:World_Bright-Earth-e-Atlas-basemap_No-Labels"
> }
> ```


#### NETCDF and GRIB2 layer

| Field                  | Type                                                    | Necessity                    | Description |
| ---------------------- | ------------------------------------------------------- | ---------------------------- | ----------- |
| `input`                | [`INPUT`](#Datatype-INPUT)                              | Mandatory                    | Configuration of the input object, used to locate the NetCDF / GRIB2 files the layer should use. |
| `variable`             | [`VARIABLE`](#Datatype-VARIABLE)                        | Optional                     | Define a variable to raster |
| `arrowVariable`        | [`VARIABLE`](#Datatype-VARIABLE)                        | Optional                     | Define a variable to use to draw arrows. |
| `trueColourVariables`  | Array of [`VARIABLE`](#Datatype-VARIABLE) *or* Object of <String, [`VARIABLE`](#Datatype-VARIABLE)> | Optional | Define the variables and the colour range used to create a true colour layer. See example bellow. |
| `targetHeight`         | String                                                  | Optional                     | Target height used to select the data from the NetCDF / GRIB2 file. Can be a fixed value: "-1.5" or a pattern, to get the current target height "${ctx.targetHeight}" |
| `arrowSize`            | Integer                                                 | Optional: Default: `20`      | Used with layers that display arrows. Set to smaller value to get a higher density of smaller arrows. |


> **NOTE:** At least one of `variable`, `arrowVariable` or `trueColourVariables` must be defined
> otherwise the layer is ignored.
> The layer config part usually define no variable.
> The variable is set with a layer overwrite when placed in a panel,
> to make the layer more reusable.

> **Raster example**
> 
> ```
> {
>     "_id": {
>         "id": "default",
>         "datatype": "LAYER"
>     },
>     "lastModified": "2019-08-15T12:25:00.000+08:00",
> 
>     "input": "downloads/gbr4_v2",
>     "type": "NETCDF",
> 
>     "targetHeight": "${ctx.targetHeight}",
>     "variable": "ereefs/gbr4_v2/temp"
> }
> ```

> **True colour example**
> 
> ```
> {
>     "_id": {
>         "id": "default",
>         "datatype": "LAYER"
>     },
>     "lastModified": "2019-08-15T12:25:00.000+08:00",
> 
>     "input": "downloads/gbr4_bgc_924",
>     "type": "NETCDF"
> 
>     "trueColourVariables": {
>         "R_470": {
>             "variableId": "R_470",
>             "hexColours": [
>                 "#000001",
>                 "#00005e",
>                 "#000091",
>                 "#0000ae",
>                 "#0000c3",
>                 "#0000d5",
>                 "#0000e0",
>                 "#0000eb",
>                 "#0000f3",
>                 "#0000f9",
>                 "#0000ff"
>             ],
>             "scaleMin": 0,
>             "scaleMax": 0.1
>         },
>         "R_555": {
>             "variableId": "R_555",
>             "hexColours": [
>                 "#000100",
>                 "#005e00",
>                 "#009100",
>                 "#00ae00",
>                 "#00c300",
>                 "#00d500",
>                 "#00e000",
>                 "#00eb00",
>                 "#00f300",
>                 "#00f900",
>                 "#00ff00"
>             ],
>             "scaleMin": 0,
>             "scaleMax": 0.1
>         },
>         "R_645": {
>             "variableId": "R_645",
>             "hexColours": [
>                 "#010000",
>                 "#5e0000",
>                 "#910000",
>                 "#ae0000",
>                 "#c30000",
>                 "#d50000",
>                 "#e00000",
>                 "#eb0000",
>                 "#f30000",
>                 "#f90000",
>                 "#ff0000"
>             ],
>             "scaleMin": 0,
>             "scaleMax": 0.1
>         }
>     }
> }
> ```


### Datatype REGION

| Field                  | Type                                                | Necessity                    | Description |
| ---------------------- | --------------------------------------------------- | ---------------------------- | ----------- |
| `label`                | String                                              | Mandatory                    | The name of the region. |
| `bbox`                 | [`BBOX`](#BBOX)                                     | Mandatory                    | The bounding box of the region. |


> **Example**
> 
> ```
> {
>     "_id": {
>         "id": "default",
>         "datatype": "REGION"
>     },
>     "lastModified": "2019-08-15T12:25:00.000+08:00",
> 
>     "label": "Queensland",
>     "bbox": {
>         "east": 156,
>         "north": -7.6,
>         "south": -29.4,
>         "west": 142.4
>     }
> }
> ```


### Datatype INPUT

| Field                  | Type                                                | Necessity                    | Description |
| ---------------------- | --------------------------------------------------- | ---------------------------- | ----------- |
| `timeIncrement`        | [`TIME_INCREMENT`](#TIME_INCREMENT)                 | Mandatory                    | Expected elapse time between data date. |
| `licence`              | String                                              | Optional                     | What licence is attached to the files. This can be used with `variables` to display the frame licence, which is a cumulative list of all data licence used to generate the frame. |
| `author`               | String                                              | Optional                     | The author of the data. This can be used with `variables` to display the frame authors, which is a cumulative list of all data authors used to generate the frame. |
| `authors`              | Array of String                                     | Optional                     | The list of author of the data. Used instead of `author` when the data have multiple authors. |

> **NOTE:** The `timeIncrement` must be set.
> It can't be automatically determined from the data because some data files
> only contains one date of data, and some others have missing dates.
> 
> For example, NOAA data files have data every 3 hours. Therefore, this field must be set to the following for NOAA's `INPUT`:
> 
> ```
> "timeIncrement": {
>     "increment": 3,
>     "unit": "HOUR"
> }
> ```

> **Example**
> 
> ```
> {
>     "_id": {
>         "id": "default",
>         "datatype": "INPUT"
>     },
>     "lastModified": "2019-08-15T12:25:00.000+08:00",
> 
>     "timeIncrement": {
>         "increment": 1,
>         "unit": "HOUR"
>     },
>     "licence": "CC-BY 4.0",
>     "authors": [
>         "AIMS",
>         "eReefs CSIRO GBR4 Hydrodynamic Model v2.0"
>     ]
> }
> ```


### Datatype VARIABLE

| Field                  | Type                                                | Necessity                      | Description |
| ---------------------- | --------------------------------------------------- | ------------------------------ | ----------- |
| `variableId`           | String                                              | Mandatory                      | The ID of the variable, as defined in the NetCDF / GRIB2 file. |
| `colourPaletteName`    | String                                              | Optional. Default: `x-Rainbow` | Colour palette used with the `edal` library to raster the layer. See bellow for more information. |
| `scaleMin`             | Float                                               | Optional. Default: `-50.0`     | Minimum value in the scale. |
| `scaleMax`             | Float                                               | Optional. Default: `+50.0`     | Maximum value in the scale. |
| `northAngle`           | Float                                               | Optional. Default: `0`         | Angle pointing north, in degree. Used with non-standard `dir` variable. See bellow for more information. |
| `directionTurns`       | Float                                               | Optional. Default: `360`       | Number of unit in a circle. Used with non-standard `dir` variable. See bellow for more information. |
| `logarithmic`          | Boolean                                             | Optional. Default: `false`     | Use logarithmic scale. |
| `legend`               | [`LEGEND`](#Datatype-LEGEND)                        | Optional. Default: No legend   | Define where and how the legend is rendered. |
| `arrowColour`          | String                                              | Optional. Default: `#000000`   | Colour of the arrow, used with `arrowVariable`. |
| `colourSchemeType`     | String                                              | Optional. Default: `scale`     | Type of legend. Accepted values are: `scale`, `thresholds` and `arrow_thresholds`. |
| `arrowThresholds`      | Array of String                                     | Optional                       | List of colour, used with `arrowVariable`, when `colourSchemeType` is set to `arrow_thresholds`. See bellow for more information. |
| `thresholds`           | Array of Float                                      | Optional                       | Define the thresholds to use in the legend, when `colourSchemeType` is set to `thresholds`. See bellow for more information. |

**Field `colourPaletteName`**

Custom palettes can be added. Simply create a file with the `.pal` extension and place it in the `paletteDirectoryUri` directory.
The palette name can be used by putting the filename without the `.pal` extension in the `colourPaletteName` field.

Format of the palette files:

```
% Comment (it's recommended to put a human readable palette name here, for maintenance)
#FF0000FF
#FF00FF00
#FFFF0000
```

where each colour is in the following format:

`#AARRGGBB`
or
`#RRGGBB`

- `AA`: Hexadecimal code for the *alpha* channel. `00` for transparent, `FF` for opaque. Optional.
- `RR`: Hexadecimal code for the *red* channel.
- `GG`: Hexadecimal code for the *green* channel.
- `BB`: Hexadecimal code for the *blue* channel.

Lines starting with a `%` are comments.

You can find the list of default `edal` colour palettes from the GitHub repository:

https://github.com/Reading-eScience-Centre/edal-java/tree/master/graphics/src/main/resources/palettes

> **NOTE**: When setting up a variable in NcAnimate config,
> the colour palette can be followed by the suffix `-inv`
> to invert the order of colours in the palette.
> There is no need to duplicate the colour palette file.  
> Example: `"colourPaletteName": "x-Rainbow-inv"`


**Fields `northAngle` and `directionTurns`**

NetCDF files usually have vector variables grouped together. This is used by the `edal` library to create virtual
variables with the following suffix:
- `-group`
- `-dir`
- `-mag`

The variable with the suffix `-group` can be used directly in NcAnimate configuration
to define a raster layer with arrows.

Some files, notably GRIB2 files, only contains one variable per file, making that grouping impossible.

If a variable define a direction, some extra information can be provided to describe how the variable is set up.
- `northAngle`: Angle pointing North, in degree. The virtual variable `-dir` define North as angle `0`.
    If angle `0` is pointing South, `northAngle` needs to be set to `180` degree.
- `directionTurns`: The number of unit in a full circle. This is used to cover every angular units possible.
    For degree, use `360`.
    For radian, use 2*PI (approximately `6.2831853`).
    If the direction variable is defined counter-clockwise, the `directionTurns` can be set
    as a negative number to compensate for this.
    For other angular units, refer to the wiki page: https://en.wikipedia.org/wiki/Angular_unit


> **Raster variable example**
> 
> ```
> {
>     "_id": {
>         "id": "default",
>         "datatype": "VARIABLE"
>     },
>     "lastModified": "2019-08-15T12:25:00.000+08:00",
> 
>     "variableId": "temp",
>     "legend": {
>         "title": { "text": "Temperature (°C)" }
>     },
>     "colourPaletteName": "x-Rainbow",
>     "scaleMax": 34,
>     "scaleMin": 22
> }
> ```

> **Vector variable example** (NetCDF only)
> 
> ```
> {
>     "_id": {
>         "id": "default",
>         "datatype": "VARIABLE"
>     },
>     "lastModified": "2019-08-15T12:25:00.000+08:00",
> 
>     "variableId": "wspeed_u:wspeed_v-group",
>     "legend": {
>         "title": { "text": "Magnitude of wind (ms⁻¹)" }
>     },
>     "colourPaletteName": "x-Rainbow",
>     "scaleMax": 20,
>     "scaleMin": 0
> }
> ```

**Field `arrowThresholds`**

List of thresholds and colours, used to define an arrowVariable rendered with colours.

Each threshold must be defined between 2 colours.
Therefore, the content of this field alternate between colour and threshold value:
> [HEXADECIMAL COLOUR]  
> [MAGNITUDE THRESHOLD VALUE]  
> [HEXADECIMAL COLOUR]  
> [MAGNITUDE THRESHOLD VALUE]  
> [HEXADECIMAL COLOUR]  
> ...

> **arrowThresholds example**
> 
> ```
> {
>     "id": "wind",
>     "layerOverwrites": {
>         "ereefs-model_gbr4-v2": {
>             "variable": {
>                 "id": "ereefs/gbr4_v2/wind"
>             },
>             "arrowVariable": {
>                 "id": "ereefs/gbr4_v2/current",
>                 "arrowThresholds": [
>                     "#00000066",
>                     "0.4",
>                     "#00990066",
>                     "0.45",
>                     "#99990066",
>                     "0.5",
>                     "#99000066"
>                 ]
>             }
>         }
>     }
> }
> ```

In the above example:
- Black arrow: Magnitude = `[-infinity, 0.4]`
- Green arrow: Magnitude = `]0.4, 0.45]`
- Yellow arrow: Magnitude = `]0.45, 0.5]`
- Red arrow: Magnitude = `]0.5, infinity]`

**Fields `colourSchemeType` and `thresholds`**

To define thresholds in the legend, set the `colourSchemeType` to `thresholds` and define a list of `thresholds`.

> **thresholds example**
> 
> ```
> {
>     "_id": {
>         "id": "ereefs/gbr1_2-0-river",
>         "datatype": "VARIABLE"
>     },
>     "variableId": "bur",
>     "legend": {
>         "steps": 6,
>         "colourBandColourCount": 5,
>     },
>     "colourSchemeType": "thresholds",
>     "thresholds": [0.0015, 0.02, 0.1, 0.3]
> }
> ```



### Datatype RENDER

| Field                  | Type                                                | Necessity                      | Description |
| ---------------------- | --------------------------------------------------- | ------------------------------ | ----------- |
| `directoryUri`         | String (URI)                                        | Mandatory                      | URI of directory where generated products (videos and maps) will be saved. Supports S3 URI and local file URI. Example: "s3://bucket/products/${id}" |
| `paletteDirectoryUri`  | String (URI)                                        | Optional                       | Define the directory where custom NetCDF colour palettes can be found. Supports S3 URI and local file URI. Examples: "/tmp/colourPalettes", "s3://bucket/colourPalettes" |
| `frameDirectoryUri`    | String (URI)                                        | Optional                       | URI of directory used to archive generated frames. Those frames can be reused if the product has to be regenerated and the frames are not outdated. Supports S3 URI and local file URI. Example: "s3://bucket/frames/${id}" |
| `workingDirectory`     | String (File path)                                  | Optional. Default: `/tmp/ncanimate` | Path to a local folder where input files are downloaded and output files are generated before been uploaded to the `directoryUri`. It is advised to set this field to a directory on a disk which have plenty of free disk space. |
| `ncanimateFrameJar`    | String (File path)                                  | Optional                       | Path to the folder containing the JAR file `ereefs-ncanimate2-frame-X.X-jar-with-dependencies.jar`. If not specified, NcAnimate will look for the jar in the same folder as NcAnimate JAR file reside. Example: "../ereefs-ncanimate2-frame/target" |
| `scale`                | Float                                               | Optional. Default: `1.0`       | Used to increase / decrease the resolution of the generated frames. |
| `definitionId`         | String                                              | Optional                       | Used to group NcAnimate metadata. For example, the NcAnimate configurations `temp-range_hourly` and `temp-range_daily` can be grouped together using `"definitionId": "temp-range"`. |
| `timezone`             | String                                              | Optional. Default: `UTC`       | The timezone used to interpret dates in input files and display dates on the generated product. Example: "Australia/Brisbane". List of supported timezone: http://joda-time.sourceforge.net/timezones.html |
| `frameTimeIncrement`   | [`TIME_INCREMENT`](#TIME_INCREMENT)                 | Optional                       | The time period of each frame. By default, NcAnimate will uses the smallest of all time increment found from all NetCDF / GRIB2 input files used by the NcAnimate configuration. If the detected frame increment doesn't suit your needs, you can overwrite it with this field. |
| `videoTimeIncrement`   | [`TIME_INCREMENT`](#TIME_INCREMENT)                 | Mandatory (with `videos`)      | The time period of each video. This field is only mandatory if the field `videos` is used. |
| `startDate`            | String (Date)                                       | Optional                       | Only generate product from this date. |
| `endDate`              | String (Date)                                       | Optional                       | Only generate product up to this date. |
| `videos`               | Map <String, [`video`](#video) Object>              | Optional                       | Define videos to generate. See sub-section [`video`](#video). |
| `maps`                 | Map <String, [`map`](#map) Object>                  | Optional                       | Define maps to generate. See sub-section [`map`](#map). |
| `metadata`             | [`metadata`](#metadata) Object                      | Optional                       | Define properties to add to the generated product metadata. See sub-section [`metadata`](#metadata). |

> **NOTE:** At least one `video` or one `map` needs to be defined. Both `maps` and `videos` can be defined at the same time.


#### `video`

| Field                  | Type                                                | Necessity                      | Description |
| ---------------------- | --------------------------------------------------- | ------------------------------ | ----------- |
| `maxWidth`             | Integer                                             | Optional                       | Used to specify video max dimensions, which is used to provide appropriate variables that can be used to generate de video. This field doesn't automatically resize the video frames. |
| `maxHeight`            | Integer                                             | Optional                       | Used to specify video max dimensions, which is used to provide appropriate variables that can be used to generate de video. This field doesn't automatically resize the video frames. |
| `format`               | Enum                                                | Mandatory                      | Format of the video file. Supported format: `WMV`, `MP4`, `ZIP` |
| `blockSize`            | Array of Integer                                    | Optional                       | Some video format requires that each frame size (in pixels) is a multiple of a given number. For example, MP4 baseline require frames width and height to be a multiple of 16. Therefore, the `blockSize` should be set to [16, 16] for those videos. NOTE: This will not resize the video frames. It will provide a ${ctx.padding} variable that can be used to appropriately place the video frames. See example bellow. |
| `fps`                  | Integer                                             | Optional                       | This field set the variable "${ctx.renderFile.fps} that can be used to render the video and generate the video metadata. |
| `commandLines`         | Array of String                                     | Mandatory                      | The command line to execute to generate the video. The video is generated using external tools, such as `ffmpeg`, which needs to be installed on the computer running NcAnimate. See example bellow. |


#### `map`

| Field                  | Type                                                | Necessity                      | Description |
| ---------------------- | --------------------------------------------------- | ------------------------------ | ----------- |
| `maxWidth`             | Integer                                             | Optional                       | Used to automatically resize the image file before uploading it to `directoryUri`. |
| `maxHeight`            | Integer                                             | Optional                       | Used to automatically resize the image file before uploading it to `directoryUri`. |
| `format`               | Enum                                                | Mandatory                      | Format of the map file. Supported format: `SVG`, `PNG`, `GIF`, `JPG` |


#### `metadata`

| Field                  | Type                                                | Necessity                      | Description |
| ---------------------- | --------------------------------------------------- | ------------------------------ | ----------- |
| `properties`           | Map<String, Object>                                 | Optional                       | Used to add extra information to generated JSON metadata. The map values can be any valid JSON type (String, Integer, Float, JSONArray, JSONObject). |


> **Example**
> 
> ```
> {
>     "_id": {
>         "id": "default",
>         "datatype": "RENDER"
>     },
>     "lastModified": "2019-08-15T12:25:00.000+08:00",
> 
>     "workingDirectory": "/tmp/ncanimateTests/working",
>     "paletteDirectoryUri": "s3://bucket/ncanimate/resources/palettes",
>     "frameDirectoryUri": "s3://bucket/ncanimate/frames/${id}",
>     "directoryUri": "s3://bucket/ncanimate/products/${id}",
>     "timezone": "Australia/Brisbane",
> 
>     "scale": 0.5,
> 
>     "videoTimeIncrement": {
>         "increment": 1,
>         "unit": "MONTH"
>     },
> 
>     "videos": {
>         "mp4Video": {
>             "format": "MP4",
>             "fps": 20,
>             "blockSize": [16, 16],
>             "commandLines": [
>                 "/usr/bin/ffmpeg -y -r \"${ctx.renderFile.fps}\" -i \"${ctx.videoFrameDirectory}/${ctx.frameFilenamePrefix}_%05d.png\" -vcodec libx264 -profile:v baseline -pix_fmt yuv420p -crf 29 -vf \"pad=${ctx.productWidth}:${ctx.productHeight}:${ctx.padding.left}:${ctx.padding.top}:white\" \"${ctx.outputDirectory}/temp_${ctx.outputFilename}\"",
>                 "/usr/bin/qt-faststart \"${ctx.outputDirectory}/temp_${ctx.outputFilename}\" \"${ctx.outputFile}\"",
>                 "rm \"${ctx.outputDirectory}/temp_${ctx.outputFilename}\""
>             ]
>         },
>         "wmvVideo": {
>             "format": "WMV",
>             "fps": 20,
>             "commandLines": ["/usr/bin/ffmpeg -y -r \"${ctx.renderFile.fps}\" -i \"${ctx.videoFrameDirectory}/${ctx.frameFilenamePrefix}_%05d.png\" -qscale 10 -s ${ctx.productWidth}x${ctx.productHeight} \"${ctx.outputFile}\""],
>             "maxWidth": 1280
>         },
>         "zipArchive": {
>             "format": "ZIP"
>         }
>     },
> 
>     "metadata": {
>         "properties": {
>             "targetHeight": "${ctx.targetHeight}",
>             "framePeriod": "${ctx.framePeriod}"
>         }
>     }
> }
> ```


### Others

The configuration is build using common structure share across different sections.
Those can't be specified as a config part, therefore do not have an `_id` object nor a `lastModified` field.

#### TEXT

| Field                  | Type                                   | Necessity                           | Description |
| ---------------------- | -------------------------------------- | ----------------------------------- | ----------- |
| `text`                 | String *or* Array of String            | Optional                            | The text to render. The `TEXT` element will be ignored if there is no `text` to render or the `text` is an empty String. If an Array of String is provided, the extra Strings are used as alternative texts, in the order provided: if a substitution variable from the first `text` String is not found, the next `text` String will used, and so on. If none of the `text` String can be successfully substituted, the last String is used, with the substitutions of variables that can be found. |
| `fontSize`             | Integer                                | Optional. Default: `20`             | Font size, used to instantiate a `java.awt.Font` object. |
| `fontColour`           | String                                 | Optional. Default: `#000000`        | Colour used to render the `text`. |
| `bold`                 | Boolean                                | Optional. Default: `false`          | Make text bold. |
| `italic`               | Boolean                                | Optional. Default: `false`          | Make text italic. |
| `position`             | [`POSITION`](#POSITION)                | Optional. Default: text is centered | Position the text. |

> **NOTE:** To disable the text element when a placeholder variable from the `text` String is not found,
> simply add an empty String to the array of String:
>
> ```
> {
>     "text": [
>         "Region: ${ctx.region.label}",
>         ""
>     ],
>     ...
> }
> ```


> **Example**
> 
> ```
> {
>     "text": [
>         "Region: ${ctx.region.label}",
>         "Region: ${ctx.region.id}",
>         "Region: UNKNOWN"
>     ],
>     "fontSize": 25,
>     "bold": true,
>     "italic": false
>     "position": {
>         "top": 28,
>         "left": 16
>     }
> }
> ```


#### POSITION

| Field                  | Type                                   | Necessity                           | Description |
| ---------------------- | -------------------------------------- | ----------------------------------- | ----------- |
| `top`                  | Integer                                | Optional                            | Number of pixel from the top. |
| `bottom`               | Integer                                | Optional                            | Number of pixel from the bottom. |
| `left`                 | Integer                                | Optional                            | Number of pixel from the left. |
| `right`                | Integer                                | Optional                            | Number of pixel from the right. |
| `pos`                  | Integer                                | Optional                            | Which property to consider. This is used with overwrites. See explanation bellow. |

**Field `pos`**

Position should only specify `top` or `bottom` but not both, `left` or `right` but not both.
Unfortunately, the overwrite system can't be used to remove fields (because the database does not allow saving
null value).

For example, if you try to overwrite the following position:
```
{
    "top": 28,
    "left": 16
}
```
with:
```
{
    "bottom": 28
}
```
the result would be:
```
{
    "top": 28,
    "bottom": 28,
    "left": 16
}
```
and the system would have no way to know if the text should be aligned from the top or from the bottom.
To solve this issue, the `POSITION` element have a `pos` attribute which can be used to specify which fields to consider.

The `pos` attribute is composed of the following letters:
- `t`: top
- `b`: bottom
- `l`: left
- `r`: right

To come back to our example:
```
{
    "top": 28,
    "left": 16
}
```
overwritten with:
```
{
    "bottom": 28,
    "pos": "bl"
}
```
would result in:
```
{
    "top": 28,
    "bottom": 28,
    "left": 16,
    "pos": "bl"
}
```
which is equivalent to:
```
{
    "bottom": 28,
    "left": 16
}
```

> **Example**
> 
> ```
> {
>     "top": 28,
>     "left": 16
> }
> ```


#### PADDING

Used to generate space around an element's content.

| Field                  | Type                                   | Necessity                           | Description |
| ---------------------- | -------------------------------------- | ----------------------------------- | ----------- |
| `top`                  | Integer                                | Optional                            | Top padding, in pixel. |
| `bottom`               | Integer                                | Optional                            | Bottom padding, in pixel. |
| `left`                 | Integer                                | Optional                            | Left padding, in pixel. |
| `right`                | Integer                                | Optional                            | Right padding, in pixel. |


> **Example**
> 
> ```
> {
>     "top": 80,
>     "left": 16,
>     "bottom": 49,
>     "right": 16
> }
> ```


#### BBOX 

Describe a bounding box, in degree of longitude and latitude.

| Field                  | Type                                  | Necessity                           | Description |
| ---------------------- | ------------------------------------- | ----------------------------------- | ----------- |
| `east`                 | Double                                | Optional                            | East longitude, in degree. |
| `west`                 | Double                                | Optional                            | West longitude, in degree. |
| `north`                | Double                                | Optional                            | North latitude, in degree. |
| `south`                | Double                                | Optional                            | South latitude, in degree. |

> **Example**
> 
> ```
> {
>     "east": 156,
>     "north": -22.5,
>     "south": -28.7,
>     "west": 152.13
> }
> ```

#### TIME_INCREMENT

| Field                  | Type                                                | Necessity                    | Description |
| ---------------------- | --------------------------------------------------- | ---------------------------- | ----------- |
| `unit`                 | Enum                                                | Mandatory                    | `ETERNITY`, `YEAR`, `MONTH`, `WEEK`, `DAY`, `HOUR`, `MINUTE`, `SECOND` |
| `increment`            | Integer                                             | Mandatory                    | Number of `unit` per increment. |

> **Example**
> 
> To represent an increment of 3 hours:
> ```
> {
>     "increment": 3,
>     "unit": "HOUR"
> }
> ```

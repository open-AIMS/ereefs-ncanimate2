<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0"
  xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
  xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

  <NamedLayer>
    <Name>AU_GA_GEODATA-TOPO-5M_aus5fgd_r</Name>
    <UserStyle>
      <Name>AU_GA_GEODATA-TOPO-5M_aus5fgd_r</Name>
      <Title>Australia</Title>
      <!-- ================ POLYGONS ================== -->
      <FeatureTypeStyle>

        <!-- Remove small reefs when zoomed out -->
        <Rule>
          <Name>Australia</Name>
          <Title>Australia</Title>
          <PolygonSymbolizer>
            <Fill>
              <CssParameter name="fill">#F7F7EB</CssParameter>
              <CssParameter name="fill-opacity">1</CssParameter>
            </Fill>
            <Stroke>
              <CssParameter name="stroke">#898989</CssParameter>
              <CssParameter name="stroke-opacity">0.8</CssParameter>
              <CssParameter name="stroke-width">1</CssParameter>
            </Stroke>
          </PolygonSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>

<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0"
  xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
  xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

  <NamedLayer>
    <Name>GBR_eAtlas_NRM_Regions_GBRMP_2012_3M</Name>
    <UserStyle>
      <Name>GBR_eAtlas_NRM_Regions_GBRMP_2012_3M</Name>
      <Title>NRM-Regions</Title>
      <!-- ================ POLYGONS ================== -->
      <FeatureTypeStyle>

        <!-- Remove small reefs when zoomed out -->
        <Rule>
          <Name>Regions</Name>
          <Title>Regions</Title>
          <ogc:Filter>
            <ogc:And>
              <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>FEAT_CODE</ogc:PropertyName>
                <ogc:Literal>mainland</ogc:Literal>
              </ogc:PropertyIsEqualTo>

              <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>NRM_REGION</ogc:PropertyName>
                <ogc:Literal>Burdekin</ogc:Literal>
              </ogc:PropertyIsEqualTo>
            </ogc:And>
          </ogc:Filter>
          <PolygonSymbolizer>
            <Fill>
              <CssParameter name="fill">#F5F3EA</CssParameter>
              <CssParameter name="fill-opacity">1</CssParameter>
            </Fill>
            <Stroke>
              <CssParameter name="stroke">#D7D7D7</CssParameter>
              <CssParameter name="stroke-opacity">1</CssParameter>
              <CssParameter name="stroke-width">1.2</CssParameter>
            </Stroke>
          </PolygonSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>

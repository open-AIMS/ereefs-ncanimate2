<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0"
  xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
  xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

  <NamedLayer>
    <Name>AU_AIMS-GA_GEODATA-TOPO-5M-2004_Rivers</Name>
    <UserStyle>
      <Name>AU_AIMS-GA_GEODATA-TOPO-5M-2004_Rivers</Name>
      <Title>Rivers</Title>
      <!-- ================ POLYGONS ================== -->
      <FeatureTypeStyle>

        <!-- Remove small reefs when zoomed out -->
        <Rule>
          <Name>SmallRivers</Name>
          <Title>Small Rivers</Title>
          <MaxScaleDenominator>7000000</MaxScaleDenominator>
          <ogc:Filter>
            <ogc:And>
              <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>DNAME</ogc:PropertyName>
                <ogc:Literal>NORTH-EAST COAST</ogc:Literal>
              </ogc:PropertyIsEqualTo>
              <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>RIVER_SCAL</ogc:PropertyName>
                <ogc:Literal>0</ogc:Literal>
              </ogc:PropertyIsEqualTo>
            </ogc:And>
          </ogc:Filter>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#9090d0</CssParameter>
              <CssParameter name="stroke-opacity">0.3</CssParameter>
              <CssParameter name="stroke-width">0.5</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>

        <Rule>
          <Name>LargeRivers</Name>
          <Title>Large Rivers</Title>
          <ogc:Filter>
            <ogc:And>
              <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>DNAME</ogc:PropertyName>
                <ogc:Literal>NORTH-EAST COAST</ogc:Literal>
              </ogc:PropertyIsEqualTo>
              <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>RIVER_SCAL</ogc:PropertyName>
                <ogc:Literal>1</ogc:Literal>
              </ogc:PropertyIsEqualTo>
            </ogc:And>
          </ogc:Filter>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#9090d0</CssParameter>
              <CssParameter name="stroke-opacity">0.6</CssParameter>
              <CssParameter name="stroke-width">1</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>

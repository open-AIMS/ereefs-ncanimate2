<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0"
  xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
  xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

  <NamedLayer>
    <Name>river_catchments</Name>
    <UserStyle>
      <Name>river_catchments</Name>
      <Title>River catchments</Title>
      <FeatureTypeStyle>

        <!-- Normanby -->
        <Rule>
          <Name>NormanbyCatchment</Name>
          <Title>Normanby catchment</Title>

          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
              <ogc:PropertyName>BNAME</ogc:PropertyName>
              <ogc:Literal>NORMANBY RIVER</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>

          <PolygonSymbolizer>
            <Fill>
              <CssParameter name="fill">#850149</CssParameter>
              <CssParameter name="fill-opacity">0.1</CssParameter>
            </Fill>
          </PolygonSymbolizer>
        </Rule>
        
        <!-- Mulgrave -->
        <Rule>
          <Name>MulgraveCatchment</Name>
          <Title>Mulgrave catchment</Title>

          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
              <ogc:PropertyName>BNAME</ogc:PropertyName>
              <ogc:Literal>MULGRAVE-RUSSELL RIVERS</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          
          <PolygonSymbolizer>
            <Fill>
              <CssParameter name="fill">#425C00</CssParameter>
              <CssParameter name="fill-opacity">0.1</CssParameter>
            </Fill>
          </PolygonSymbolizer>
        </Rule>
        
        <!-- Herbert -->
        <Rule>
          <Name>HerbertCatchment</Name>
          <Title>Herbert catchment</Title>
        
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
              <ogc:PropertyName>BNAME</ogc:PropertyName>
              <ogc:Literal>HERBERT RIVER</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
        
          <PolygonSymbolizer>
            <Fill>
              <CssParameter name="fill">#0000B4</CssParameter>
              <CssParameter name="fill-opacity">0.1</CssParameter>
            </Fill>
          </PolygonSymbolizer>          
        </Rule>

        <!-- Pioneer -->
        <Rule>
          <Name>PioneerCatchment</Name>
          <Title>Pioneer catchment</Title>

          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
              <ogc:PropertyName>BNAME</ogc:PropertyName>
              <ogc:Literal>PIONEER RIVER</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>

          <PolygonSymbolizer>
            <Fill>
              <CssParameter name="fill">#325960</CssParameter>
              <CssParameter name="fill-opacity">0.1</CssParameter>
            </Fill>
          </PolygonSymbolizer>
        </Rule>
        
        <!-- Mary -->
        <Rule>
          <Name>MaryCatchment</Name>
          <Title>Mary catchment</Title>

          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
              <ogc:PropertyName>BNAME</ogc:PropertyName>
              <ogc:Literal>MARY RIVER (QLD)</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>

          <PolygonSymbolizer>
            <Fill>
              <CssParameter name="fill">#69280B</CssParameter>
              <CssParameter name="fill-opacity">0.1</CssParameter>
            </Fill>
          </PolygonSymbolizer>
        </Rule>

      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>

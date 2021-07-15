<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0"
  xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
  xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

  <NamedLayer>
    <Name>World_NE_10m-cities_V3_Ranked</Name>
    <UserStyle>
      <Name>World_NE_10m-cities_V3_Ranked</Name>
      <Title>World-Cities</Title>
      <FeatureTypeStyle>

        <Rule>
          <Name>MajorCities</Name>
          <Title>Major Cities</Title>

          <ogc:Filter>
            <ogc:Or>
              <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>NAME</ogc:PropertyName>
                <ogc:Literal>Cairns</ogc:Literal>
              </ogc:PropertyIsEqualTo>

              <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>NAME</ogc:PropertyName>
                <ogc:Literal>Townsville</ogc:Literal>
              </ogc:PropertyIsEqualTo>

              <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>NAME</ogc:PropertyName>
                <ogc:Literal>Mackay</ogc:Literal>
              </ogc:PropertyIsEqualTo>

              <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>NAME</ogc:PropertyName>
                <ogc:Literal>Rockhampton</ogc:Literal>
              </ogc:PropertyIsEqualTo>

              <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>NAME</ogc:PropertyName>
                <ogc:Literal>Brisbane</ogc:Literal>
              </ogc:PropertyIsEqualTo>
            </ogc:Or>
          </ogc:Filter>

          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>circle</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#663333</CssParameter>
                  <CssParameter name="fill-opacity">1.0</CssParameter>
                </Fill>
              </Mark>
              <Size>8</Size>
            </Graphic>
          </PointSymbolizer>
          <TextSymbolizer>
            <Label>
              <ogc:PropertyName>NAME</ogc:PropertyName>
            </Label>
            <Font>
              <CssParameter name="font-family">SansSerif</CssParameter>
              <CssParameter name="font-size">22</CssParameter>
            </Font>
            <LabelPlacement>
              <PointPlacement>
                <AnchorPoint>
                  <AnchorPointX>1.15</AnchorPointX>
                  <AnchorPointY>0.5</AnchorPointY>
                </AnchorPoint>
              </PointPlacement>
            </LabelPlacement>
            <Fill>
              <CssParameter name="fill">#44343F</CssParameter>
              <CssParameter name="fill-opacity">1</CssParameter>
            </Fill>
          </TextSymbolizer>
        </Rule>

      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>

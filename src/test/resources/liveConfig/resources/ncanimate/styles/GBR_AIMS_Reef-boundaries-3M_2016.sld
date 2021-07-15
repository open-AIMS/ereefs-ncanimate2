<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0"
  xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
  xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

  <NamedLayer>
    <Name>GBR_AIMS_Reef-boundaries-3M_2016</Name>
    <UserStyle>
      <Name>GBR_AIMS_Reef-boundaries-3M_2016</Name>
      <Title>GBR-reefs</Title>
      <!-- ================ POLYGONS ================== -->
      <FeatureTypeStyle>

        <!-- Remove small reefs when zoomed out -->
        <Rule>
          <Name>Reefs</Name>
          <Title>Reefs</Title>
          <PolygonSymbolizer>
            <Fill>
              <CssParameter name="fill">#000000</CssParameter>
              <CssParameter name="fill-opacity">0.5</CssParameter>
            </Fill>
          </PolygonSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>

<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template xmlns:ns="urn:example:po" match="/">
        <PurchaseOrder xmlns="urn:example:po">
            <ShippingAddress><xsl:value-of select="ns:PurchaseOrder/ns:ShippingAddress"/></ShippingAddress>
        </PurchaseOrder>    

    </xsl:template>

</xsl:stylesheet>

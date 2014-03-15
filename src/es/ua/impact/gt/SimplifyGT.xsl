<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : SimplifyGT.xsl
    Created on : February 3, 2012, 11:55 AM
    Author     : xavi
    Description:
        Purpose of transformation follows.
-->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="TextLine" />

    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>

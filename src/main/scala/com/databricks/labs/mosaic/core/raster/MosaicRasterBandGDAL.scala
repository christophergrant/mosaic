package com.databricks.labs.mosaic.core.raster

import org.gdal.gdal.Band
import org.gdal.gdalconst.gdalconstConstants

import scala.collection.JavaConverters.dictionaryAsScalaMapConverter
import scala.util._

case class MosaicRasterBandGDAL(band: Band, id: Int) extends MosaicRasterBand {

    override def index: Int = id

    override def description: String = coerceNull(Try(band.GetDescription))

    override def metadata: Map[String, String] =
        Option(band.GetMetadata_Dict)
            .map(_.asScala.toMap.asInstanceOf[Map[String, String]])
            .getOrElse(Map.empty[String, String])

    override def units: String = coerceNull(Try(band.GetUnitType))

    def coerceNull(tryVal: Try[String]): String = tryVal.filter(_ != null).getOrElse("")

    override def dataType: Int = Try(band.getDataType).getOrElse(0)

    override def xSize: Int = Try(band.GetXSize).getOrElse(0)

    override def ySize: Int = Try(band.GetYSize).getOrElse(0)

    override def minPixelValue: Double = computeMinMax.head

    override def maxPixelValue: Double = computeMinMax.last

    def computeMinMax: Seq[Double] = {
        val minMaxVals = Array.fill[Double](2)(0)
        Try(band.ComputeRasterMinMax(minMaxVals, 0))
            .map(_ => minMaxVals.toSeq)
            .getOrElse(Seq(Double.NaN, Double.NaN))
    }

    override def noDataValue: Double = {
        val noDataVal = Array.fill[java.lang.Double](1)(0)
        band.GetNoDataValue(noDataVal)
        noDataVal.head
    }

    override def values(xOffset: Int, yOffset: Int, xSize: Int, ySize: Int): Array[Array[Double]] = {
        val flatArray = Array.ofDim[Double](xSize * ySize)
        (xSize, ySize) match {
            case (0, 0) => Array(flatArray)
            case _      =>
                band.ReadRaster(xOffset, yOffset, xSize, ySize, xSize, ySize, gdalconstConstants.GDT_Float64, flatArray, 0, 0)
                flatArray.grouped(xSize).toArray
        }
    }

    override def pixelValueToUnitValue(pixelValue: Double): Double = (pixelValue * pixelValueScale) + pixelValueOffset

    override def pixelValueScale: Double = {
        val scale = Array.fill[java.lang.Double](1)(0)
        Try(band.GetScale(scale))
            .map(_ => scale.head.doubleValue())
            .getOrElse(0.0)
    }

    override def pixelValueOffset: Double = {
        val offset = Array.fill[java.lang.Double](1)(0)
        Try(band.GetOffset(offset))
            .map(_ => offset.head.doubleValue())
            .getOrElse(0.0)
    }

}

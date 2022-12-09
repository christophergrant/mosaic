package com.databricks.labs.mosaic.core.raster

import com.databricks.labs.mosaic.test.mocks
import org.apache.spark.sql.test.SharedSparkSessionGDAL
import org.scalatest.matchers.should.Matchers._

class TestRasterBandGDAL extends SharedSparkSessionGDAL {

    test("Read band metadata and pixel data from GeoTIFF file.") {
        val testRaster = MosaicRasterGDAL.fromBytes(mocks.geotiffBytes)

        val testBand = testRaster.getBand(1)
        testBand.asInstanceOf[MosaicRasterBandGDAL].band
        testBand.index shouldBe 1
        testBand.units shouldBe ""
        testBand.description shouldBe "Nadir_Reflectance_Band1"
        testBand.dataType shouldBe 3
        testBand.xSize shouldBe 2400
        testBand.ySize shouldBe 2400
        testBand.noDataValue shouldBe 32767.0
        (testBand.minPixelValue, testBand.maxPixelValue) shouldBe (0d, 6940d)
        (testBand.pixelValueScale, testBand.pixelValueOffset) shouldBe (1.0e-4, 0d)
        testBand.pixelValueToUnitValue(100) shouldBe 100E-4

        val testValues = testBand.values(1000, 1000, 100, 50)
        testValues.length shouldBe 50
        testValues.head.length shouldBe 100

        testRaster.cleanUp()
    }

    test("Read band metadata and pixel data from a GRIdded Binary file.") {
        val testRaster = MosaicRasterGDAL.fromBytes(mocks.gribBytes)

        val testBand = testRaster.getBand(1)
        testBand.description shouldBe "1[-] HYBL=\"Hybrid level\""
        testBand.dataType shouldBe 7
        (testBand.minPixelValue, testBand.maxPixelValue) shouldBe (1.1368277910150937e-6, 1.2002082030448946e-6)
        (testBand.pixelValueScale, testBand.pixelValueOffset) shouldBe (0d, 0d)

        val testValues = testBand.values(1, 1, 4, 5)
        testValues.length shouldBe 5
        testValues.head.length shouldBe 4


        testRaster.cleanUp()
    }

    test("Read band metadata and pixel data from a NetCDF file.") {
        val superRaster = MosaicRasterGDAL.fromBytes(mocks.netcdfBytes)
        val subdatasetPath = superRaster.subdatasets.filterKeys(_.contains("bleaching_alert_area")).head._1
        val testRaster = MosaicRasterGDAL.fromBytes(mocks.netcdfBytes, subdatasetPath)

        val scale1 = superRaster.metadata.filter(_._1.toLowerCase().contains("_quantification_value"))
        val scale = testRaster.metadata.filter(_._1.toLowerCase().contains("_quantification_value"))
        val testBand = testRaster.getBand(1)
        testBand.dataType shouldBe 1
        (testBand.minPixelValue, testBand.maxPixelValue) shouldBe (0d, 251d)
        (testBand.pixelValueScale, testBand.pixelValueOffset) shouldBe (0d, 0d)

        val testValues = testBand.values(5000, 1000, 100, 10)
        testValues.length shouldBe 10
        testValues.head.length shouldBe 100

        testRaster.cleanUp()
        superRaster.cleanUp()
    }

}

package com.databricks.labs.mosaic.core.geometry.multipoint

import com.databricks.labs.mosaic.core.geometry._
import com.databricks.labs.mosaic.core.geometry.point.{MosaicPoint, MosaicPointJTS}
import com.databricks.labs.mosaic.core.types.model._
import com.databricks.labs.mosaic.core.types.model.GeometryTypeEnum.MULTIPOINT
import com.esotericsoftware.kryo.io.Input
import org.locationtech.jts.geom._

import org.apache.spark.sql.catalyst.InternalRow

class MosaicMultiPointJTS(multiPoint: MultiPoint) extends MosaicGeometryJTS(multiPoint) with MosaicMultiPoint {

    // noinspection DuplicatedCode
    override def toInternal: InternalGeometry = {
        val points = asSeq.map(_.coord).map(InternalCoord(_))
        new InternalGeometry(MULTIPOINT.id, Array(points.toArray), Array(Array(Array())))
    }

    override def asSeq: Seq[MosaicPoint] = {
        for (i <- 0 until multiPoint.getNumPoints) yield MosaicPointJTS(multiPoint.getGeometryN(i).getCoordinates.head)
    }

    override def getBoundary: MosaicGeometry = MosaicGeometryJTS(multiPoint.getBoundary)

}

object MosaicMultiPointJTS extends GeometryReader {

    def apply(geom: Geometry): MosaicMultiPointJTS = new MosaicMultiPointJTS(geom.asInstanceOf[MultiPoint])

    // noinspection ZeroIndexToHead
    override def fromInternal(row: InternalRow): MosaicGeometry = {
        val gf = new GeometryFactory()
        val internalGeom = InternalGeometry(row)
        require(internalGeom.typeId == MULTIPOINT.id)

        val points = internalGeom.boundaries.head.map(p => gf.createPoint(p.toCoordinate))
        val multiPoint = gf.createMultiPoint(points)
        new MosaicMultiPointJTS(multiPoint)
    }

    // noinspection ZeroIndexToHead
    override def fromPoints(inPoints: Seq[MosaicPoint], geomType: GeometryTypeEnum.Value = MULTIPOINT): MosaicGeometry = {
        val gf = new GeometryFactory()
        require(geomType.id == MULTIPOINT.id)

        val points = inPoints.map(p => gf.createPoint(p.coord)).toArray
        val multiPoint = gf.createMultiPoint(points)
        new MosaicMultiPointJTS(multiPoint)
    }

    override def fromWKB(wkb: Array[Byte]): MosaicGeometry = MosaicGeometryJTS.fromWKB(wkb)

    override def fromWKT(wkt: String): MosaicGeometry = MosaicGeometryJTS.fromWKT(wkt)

    override def fromJSON(geoJson: String): MosaicGeometry = MosaicGeometryJTS.fromJSON(geoJson)

    override def fromHEX(hex: String): MosaicGeometry = MosaicGeometryJTS.fromHEX(hex)

    override def fromKryo(row: InternalRow): MosaicGeometry = {
        val kryoBytes = row.getBinary(1)
        val input = new Input(kryoBytes)
        MosaicGeometryJTS.kryo.readObject(input, classOf[MosaicMultiPointJTS])
    }

}

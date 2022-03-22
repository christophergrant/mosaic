package com.databricks.labs.mosaic.core.geometry.multipolygon

import com.databricks.labs.mosaic.core.geometry._
import com.databricks.labs.mosaic.core.geometry.linestring.MosaicLineString
import com.databricks.labs.mosaic.core.geometry.point.MosaicPoint
import com.databricks.labs.mosaic.core.geometry.polygon.MosaicPolygonJTS
import com.databricks.labs.mosaic.core.types.model.{GeometryTypeEnum, InternalGeometry}
import com.databricks.labs.mosaic.core.types.model.GeometryTypeEnum.MULTIPOLYGON
import com.esotericsoftware.kryo.io.Input
import org.locationtech.jts.geom._

import org.apache.spark.sql.catalyst.InternalRow

class MosaicMultiPolygonJTS(multiPolygon: MultiPolygon) extends MosaicGeometryJTS(multiPolygon) with MosaicMultiPolygon {

    override def toInternal: InternalGeometry = {
        val n = multiPolygon.getNumGeometries
        val polygons = for (i <- 0 until n) yield MosaicPolygonJTS(multiPolygon.getGeometryN(i)).toInternal
        val boundaries = polygons.map(_.boundaries.head).toArray
        val holes = polygons.flatMap(_.holes).toArray
        new InternalGeometry(MULTIPOLYGON.id, boundaries, holes)
    }

    override def getBoundary: MosaicGeometry = MosaicGeometryJTS(multiPolygon.getBoundary)

    override def getShells: Seq[MosaicLineString] = {
        val n = multiPolygon.getNumGeometries
        val shells = for (i <- 0 until n) yield {
            val polygon = MosaicPolygonJTS(multiPolygon.getGeometryN(i).asInstanceOf[Polygon])
            polygon.getShells.head
        }
        shells
    }

    override def asSeq: Seq[MosaicGeometry] =
        for (i <- 0 until multiPolygon.getNumGeometries) yield MosaicGeometryJTS(multiPolygon.getGeometryN(i))

    override def getHoles: Seq[Seq[MosaicLineString]] = {
        val n = multiPolygon.getNumGeometries
        val holes = for (i <- 0 until n) yield {
            val polygon = MosaicPolygonJTS(multiPolygon.getGeometryN(i).asInstanceOf[Polygon])
            polygon.getHoles.head
        }
        holes
    }

}

object MosaicMultiPolygonJTS extends GeometryReader {

    override def fromInternal(row: InternalRow): MosaicGeometry = {
        val gf = new GeometryFactory()
        val internalGeom = InternalGeometry(row)

        gf.createLinearRing(gf.createLineString().getCoordinates)
        val polygons = internalGeom.boundaries.zip(internalGeom.holes).map { case (boundaryRing, holesRings) =>
            val shell = gf.createLinearRing(boundaryRing.map(_.toCoordinate))
            val holes = holesRings.map(ring => ring.map(_.toCoordinate)).map(gf.createLinearRing)
            gf.createPolygon(shell, holes)
        }
        val multiPolygon = gf.createMultiPolygon(polygons)
        MosaicMultiPolygonJTS(multiPolygon)
    }

    def apply(multiPolygon: Geometry): MosaicMultiPolygonJTS = new MosaicMultiPolygonJTS(multiPolygon.asInstanceOf[MultiPolygon])

    override def fromPoints(points: Seq[MosaicPoint], geomType: GeometryTypeEnum.Value): MosaicGeometry = {
        throw new UnsupportedOperationException("fromPoints is not intended for creating MultiPolygons")
    }

    override def fromWKB(wkb: Array[Byte]): MosaicGeometry = MosaicGeometryJTS.fromWKB(wkb)

    override def fromWKT(wkt: String): MosaicGeometry = MosaicGeometryJTS.fromWKT(wkt)

    override def fromJSON(geoJson: String): MosaicGeometry = MosaicGeometryJTS.fromJSON(geoJson)

    override def fromHEX(hex: String): MosaicGeometry = MosaicGeometryJTS.fromHEX(hex)

    override def fromKryo(row: InternalRow): MosaicGeometry = {
        val kryoBytes = row.getBinary(1)
        val input = new Input(kryoBytes)
        MosaicGeometryJTS.kryo.readObject(input, classOf[MosaicMultiPolygonJTS])
    }

}

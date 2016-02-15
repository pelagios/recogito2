/**
 * This class is generated by jOOQ
 */
package models.generated.tables.records


import java.lang.Integer
import java.lang.String

import javax.annotation.Generated

import models.generated.tables.UploadFilepart

import org.jooq.Field
import org.jooq.Record1
import org.jooq.Record4
import org.jooq.Row4
import org.jooq.impl.UpdatableRecordImpl


/**
 * This class is generated by jOOQ.
 */
@Generated(
	value = Array(
		"http://www.jooq.org",
		"jOOQ version:3.7.2"
	),
	comments = "This class is generated by jOOQ"
)
class UploadFilepartRecord extends UpdatableRecordImpl[UploadFilepartRecord](UploadFilepart.UPLOAD_FILEPART) with Record4[Integer, Integer, String, String] {

	/**
	 * Setter for <code>upload_filepart.id</code>.
	 */
	def setId(value : Integer) : Unit = {
		setValue(0, value)
	}

	/**
	 * Getter for <code>upload_filepart.id</code>.
	 */
	def getId : Integer = {
		val r = getValue(0)
		if (r == null) null else r.asInstanceOf[Integer]
	}

	/**
	 * Setter for <code>upload_filepart.upload_id</code>.
	 */
	def setUploadId(value : Integer) : Unit = {
		setValue(1, value)
	}

	/**
	 * Getter for <code>upload_filepart.upload_id</code>.
	 */
	def getUploadId : Integer = {
		val r = getValue(1)
		if (r == null) null else r.asInstanceOf[Integer]
	}

	/**
	 * Setter for <code>upload_filepart.title</code>.
	 */
	def setTitle(value : String) : Unit = {
		setValue(2, value)
	}

	/**
	 * Getter for <code>upload_filepart.title</code>.
	 */
	def getTitle : String = {
		val r = getValue(2)
		if (r == null) null else r.asInstanceOf[String]
	}

	/**
	 * Setter for <code>upload_filepart.filepath</code>.
	 */
	def setFilepath(value : String) : Unit = {
		setValue(3, value)
	}

	/**
	 * Getter for <code>upload_filepart.filepath</code>.
	 */
	def getFilepath : String = {
		val r = getValue(3)
		if (r == null) null else r.asInstanceOf[String]
	}

	// -------------------------------------------------------------------------
	// Primary key information
	// -------------------------------------------------------------------------
	override def key() : Record1[Integer] = {
		return super.key.asInstanceOf[ Record1[Integer] ]
	}

	// -------------------------------------------------------------------------
	// Record4 type implementation
	// -------------------------------------------------------------------------

	override def fieldsRow : Row4[Integer, Integer, String, String] = {
		super.fieldsRow.asInstanceOf[ Row4[Integer, Integer, String, String] ]
	}

	override def valuesRow : Row4[Integer, Integer, String, String] = {
		super.valuesRow.asInstanceOf[ Row4[Integer, Integer, String, String] ]
	}
	override def field1 : Field[Integer] = UploadFilepart.UPLOAD_FILEPART.ID
	override def field2 : Field[Integer] = UploadFilepart.UPLOAD_FILEPART.UPLOAD_ID
	override def field3 : Field[String] = UploadFilepart.UPLOAD_FILEPART.TITLE
	override def field4 : Field[String] = UploadFilepart.UPLOAD_FILEPART.FILEPATH
	override def value1 : Integer = getId
	override def value2 : Integer = getUploadId
	override def value3 : String = getTitle
	override def value4 : String = getFilepath

	override def value1(value : Integer) : UploadFilepartRecord = {
		setId(value)
		this
	}

	override def value2(value : Integer) : UploadFilepartRecord = {
		setUploadId(value)
		this
	}

	override def value3(value : String) : UploadFilepartRecord = {
		setTitle(value)
		this
	}

	override def value4(value : String) : UploadFilepartRecord = {
		setFilepath(value)
		this
	}

	override def values(value1 : Integer, value2 : Integer, value3 : String, value4 : String) : UploadFilepartRecord = {
		this.value1(value1)
		this.value2(value2)
		this.value3(value3)
		this.value4(value4)
		this
	}

	/**
	 * Create a detached, initialised UploadFilepartRecord
	 */
	def this(id : Integer, uploadId : Integer, title : String, filepath : String) = {
		this()

		setValue(0, id)
		setValue(1, uploadId)
		setValue(2, title)
		setValue(3, filepath)
	}
}

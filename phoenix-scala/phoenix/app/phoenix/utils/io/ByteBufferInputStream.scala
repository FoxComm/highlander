package utils.io

import java.io.InputStream
import java.nio.ByteBuffer

class ByteBufferInputStream(buf: ByteBuffer) extends InputStream {

  def read(): Int = {
    if (!buf.hasRemaining) return -1
    buf.get & 0xFF
  }

  override def read(bytes: Array[Byte], off: Int, len: Int): Int =
    if (!buf.hasRemaining) -1
    else {
      val readLen = Math.min(len, buf.remaining)
      buf.get(bytes, off, readLen)
      readLen
    }

  override def markSupported(): Boolean = true

  override def skip(n: Long): Long = {
    buf.position((buf.position() + n).toInt)
    n
  }

  override def available(): Int = buf.remaining

  override def mark(readAheadLimit: Int) = buf.mark()

  override def reset() = buf.reset()
}

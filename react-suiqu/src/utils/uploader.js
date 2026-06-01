import SparkMD5 from 'spark-md5'

const CHUNK_SIZE = 5 * 1024 * 1024

export function computeMD5(file, onProgress) {
  return new Promise((resolve, reject) => {
    const chunks = Math.ceil(file.size / CHUNK_SIZE)
    const spark = new SparkMD5.ArrayBuffer()
    const reader = new FileReader()
    let current = 0

    reader.onload = (e) => {
      spark.append(e.target.result)
      current++
      if (onProgress) onProgress(Math.round((current / chunks) * 100))
      if (current < chunks) {
        loadNext()
      } else {
        resolve(spark.end())
      }
    }

    reader.onerror = reject

    function loadNext() {
      const start = current * CHUNK_SIZE
      const end = Math.min(start + CHUNK_SIZE, file.size)
      reader.readAsArrayBuffer(file.slice(start, end))
    }

    loadNext()
  })
}

export function getChunkCount(file) {
  return Math.ceil(file.size / CHUNK_SIZE)
}

export { CHUNK_SIZE }

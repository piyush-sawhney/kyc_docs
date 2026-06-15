import { jsPDF } from 'jspdf'

export interface PdfConfig {
  imagesPerPage: number
  marginTop: number
  marginBottom: number
  marginLeft: number
  marginRight: number
  orientation: 'portrait' | 'landscape'
}

export interface PdfImage {
  blob: Blob
}

const PAGE_W = 210
const PAGE_H = 297

export async function generatePdf(images: PdfImage[], config: PdfConfig): Promise<Blob> {
  if (images.length === 0) throw new Error('No images to generate PDF')

  const cols = Math.ceil(Math.sqrt(config.imagesPerPage))
  const rows = Math.ceil(config.imagesPerPage / cols)

  const doc = new jsPDF({
    orientation: config.orientation,
    unit: 'mm',
    format: 'a4',
  })

  const usableW = PAGE_W - config.marginLeft - config.marginRight
  const usableH = PAGE_H - config.marginTop - config.marginBottom

  const gap = 2
  const cellW = (usableW - (cols - 1) * gap) / cols
  const cellH = (usableH - (rows - 1) * gap) / rows

  let entries: { dataUrl: string; width: number; height: number }[] = []
  try {
    entries = await Promise.all(images.map((img) => blobToJpegDataUrl(img.blob)))
  } catch {
    throw new Error('Failed to load one or more images')
  }

  for (let i = 0; i < entries.length; i++) {
    if (i > 0 && i % config.imagesPerPage === 0) {
      doc.addPage()
    }

    const { dataUrl, width, height } = entries[i]
    const posInPage = i % config.imagesPerPage
    const col = posInPage % cols
    const row = Math.floor(posInPage / cols)

    const x = config.marginLeft + col * (cellW + gap)
    const y = config.marginTop + row * (cellH + gap)
    const scale = Math.min(cellW / width, cellH / height) * 0.92
    const drawW = width * scale
    const drawH = height * scale
    const drawX = x + (cellW - drawW) / 2
    const drawY = y + (cellH - drawH) / 2

    doc.addImage(dataUrl, 'JPEG', drawX, drawY, drawW, drawH)
  }

  return doc.output('blob')
}

function blobToJpegDataUrl(blob: Blob): Promise<{ dataUrl: string; width: number; height: number }> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(blob)
    const img = new Image()
    img.onload = () => {
      URL.revokeObjectURL(url)
      const canvas = document.createElement('canvas')
      canvas.width = img.width
      canvas.height = img.height
      const ctx = canvas.getContext('2d')
      if (!ctx) { reject(new Error('Canvas 2D not available')); return }
      ctx.drawImage(img, 0, 0)
      resolve({
        dataUrl: canvas.toDataURL('image/jpeg', 0.85),
        width: img.width,
        height: img.height,
      })
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('Failed to decode image'))
    }
    img.src = url
  })
}

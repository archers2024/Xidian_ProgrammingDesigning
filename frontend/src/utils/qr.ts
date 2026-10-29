import QRCode from 'qrcode'

/** 在浏览器本地把链接画成二维码，不请求外部二维码服务。 */
export function toQrDataUrl(text: string, width = 280): Promise<string> {
  return QRCode.toDataURL(text, {
    width,
    margin: 1,
    errorCorrectionLevel: 'M',
  })
}

/**
 * Construye un link wa.me para un solo destinatario. No existe "enviar a todos"
 * con un click — es una limitación real de wa.me, no de esta app: cada link abre
 * una conversación de WhatsApp distinta que el usuario debe enviar manualmente.
 */
export function generarLinkWhatsApp(celular: string | null | undefined, mensaje: string): string | null {
  if (!celular) return null;
  const numero = celular.replace(/[^\d]/g, '');
  const numeroConPrefijo = numero.startsWith('51') ? numero : `51${numero}`;
  return `https://wa.me/${numeroConPrefijo}?text=${encodeURIComponent(mensaje)}`;
}

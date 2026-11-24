export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso);
  return match ? `${match[3]}.${match[2]}.${match[1]}` : iso;
}

export function dossierNumber(id: number | string | null | undefined): string {
  if (id === null || id === undefined || id === '') return '№ ——————';
  return '№ ' + String(id).padStart(6, '0');
}

export function formatNumber(value: number | null | undefined, maxFractionDigits = 4): string {
  if (value === null || value === undefined || Number.isNaN(value)) return '—';
  return value.toLocaleString('ru-RU', { maximumFractionDigits: maxFractionDigits });
}

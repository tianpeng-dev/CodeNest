const siteName = 'CodeNest';

export function setPageTitle(title?: string) {
  if (typeof document === 'undefined') return;

  document.title = title ? `${title} - ${siteName}` : siteName;
}

export function setPageDescription(description: string) {
  if (typeof document === 'undefined') return;

  let meta = document.querySelector<HTMLMetaElement>('meta[name="description"]');

  if (!meta) {
    meta = document.createElement('meta');
    meta.name = 'description';
    document.head.appendChild(meta);
  }

  meta.content = description;
}

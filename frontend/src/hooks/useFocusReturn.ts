import { useCallback, useRef } from 'react';

export function useFocusReturn() {
  const opener = useRef<HTMLElement | null>(null);

  const remember = useCallback(() => {
    const active = document.activeElement;
    opener.current = active instanceof HTMLElement && active !== document.body ? active : null;
  }, []);

  const restore = useCallback(() => {
    const element = opener.current;
    opener.current = null;
    setTimeout(() => {
      if (element && element.isConnected) {
        element.focus();
      }
    }, 0);
  }, []);

  return { remember, restore };
}

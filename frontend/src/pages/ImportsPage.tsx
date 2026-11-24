import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { listImports, uploadImport } from '../api/imports';
import type { ImportStatus } from '../api/imports';
import { errorMessage, isAbortError } from '../api/client';

const statusLabels: Record<ImportStatus, string> = {
  IN_PROGRESS: 'Выполняется',
  SUCCESS: 'Успешно',
  FAILED: 'Ошибка',
};

export function ImportsPage() {
  const [offset, setOffset] = useState(0);
  const [refresh, setRefresh] = useState(0);
  const [page, setPage] = useState<Awaited<ReturnType<typeof listImports>> | null>(null);
  const [historyError, setHistoryError] = useState('');
  const [pending, setPending] = useState(false);
  const [outcome, setOutcome] = useState<{ error: boolean; message: string } | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    setHistoryError('');
    setPage(null);
    listImports(offset, controller.signal)
      .then(result => { if (!controller.signal.aborted) setPage(result); })
      .catch(error => {
        if (!controller.signal.aborted && !isAbortError(error)) setHistoryError(errorMessage(error));
      });
    return () => controller.abort();
  }, [offset, refresh]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const file = new FormData(form).get('file');
    if (!(file instanceof File) || pending) return;
    setPending(true);
    setOutcome(null);
    try {
      const result = await uploadImport(file);
      setOutcome({ error: false, message: `Операция №${result.id}. Добавлено героев: ${result.addedCount}.` });
      form.reset();
    } catch (error) {
      setOutcome({ error: true, message: errorMessage(error) });
    } finally {
      setPending(false);
      setOffset(0);
      setRefresh(value => value + 1);
    }
  }

  return (
    <>
      <header className="page-head"><h1>Импорт героев</h1></header>
      <form className="op-form" onSubmit={event => void submit(event)}>
        <label className="field">
          <span>Файл JSON (до 1 МиБ, до 500 записей)</span>
          <input name="file" type="file" accept=".json,application/json" required disabled={pending} />
        </label>
        <button className="btn" disabled={pending}>{pending ? 'Импортируем…' : 'Импортировать'}</button>
      </form>
      {outcome && <p className="banner" role={outcome.error ? 'alert' : 'status'}>{outcome.message}</p>}
      <header className="page-head">
        <h2>История импорта</h2>
        <button className="btn" onClick={() => setRefresh(value => value + 1)}>Обновить</button>
      </header>
      {historyError && <p className="banner" role="alert">{historyError}</p>}
      {!page && !historyError && <p role="status">Загружаем историю…</p>}
      {page && (
        <>
          {page.items.length === 0 ? <p className="muted">Нет операций импорта.</p> : (
            <div className="table-wrap">
              <table>
                <thead><tr><th scope="col">ID</th><th scope="col">Статус</th>
                  <th scope="col">Пользователь</th><th scope="col">Добавлено героев</th></tr></thead>
                <tbody>
                  {page.items.map(item => (
                    <tr key={item.id}>
                      <td>{item.id}</td><td>{statusLabels[item.status]}</td>
                      <td>{item.username}</td><td>{item.addedCount ?? '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <div className="pager">
            <button className="btn" disabled={offset === 0}
              onClick={() => setOffset(Math.max(0, offset - page.pageSize))}>Назад</button>
            <button className="btn" disabled={!page.hasMore}
              onClick={() => setOffset(offset + page.pageSize)}>Дальше</button>
          </div>
        </>
      )}
    </>
  );
}

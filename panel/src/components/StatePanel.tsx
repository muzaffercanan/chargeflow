interface StatePanelProps {
  title: string;
  message?: string;
  actionLabel?: string;
  onAction?: () => void;
}

export function StatePanel({ title, message, actionLabel, onAction }: StatePanelProps) {
  return (
    <div className="state-panel" role={title.toLowerCase().includes("error") ? "alert" : undefined}>
      <strong>{title}</strong>
      {message && <p>{message}</p>}
      {actionLabel && onAction && (
        <button className="button secondary" type="button" onClick={onAction}>{actionLabel}</button>
      )}
    </div>
  );
}

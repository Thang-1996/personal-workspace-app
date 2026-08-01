import { UploadCloud } from 'lucide-react'
import { useRef, useState, type ChangeEvent, type DragEvent } from 'react'
import { cn } from '../../../shared/lib/cn'

type FileDropzoneProps = {
  disabled?: boolean
  progress: number | null
  onFile: (file: File) => void
}

export function FileDropzone({ disabled, onFile, progress }: FileDropzoneProps) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [dragging, setDragging] = useState(false)

  const choose = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (file) onFile(file)
    event.target.value = ''
  }

  const drop = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault()
    setDragging(false)
    const file = event.dataTransfer.files[0]
    if (file && !disabled) onFile(file)
  }

  return (
    <div
      className={cn(
        'rounded-card border-2 border-dashed p-6 text-center transition-colors',
        dragging ? 'border-brand-500 bg-brand-50' : 'border-slate-300 bg-white',
      )}
      onDragEnter={() => setDragging(true)}
      onDragLeave={() => setDragging(false)}
      onDragOver={(event) => event.preventDefault()}
      onDrop={drop}
    >
      <UploadCloud className="mx-auto text-brand-600" size={28} />
      <p className="mt-2 text-sm font-semibold text-slate-800">Drop a file here</p>
      <p className="mt-1 text-xs text-slate-500">PDF, PNG, JPEG or text; maximum 20 MB</p>
      <button
        className="mt-3 text-sm font-semibold text-brand-700 hover:underline"
        disabled={disabled}
        onClick={() => inputRef.current?.click()}
        type="button"
      >
        Browse files
      </button>
      <input
        accept=".pdf,.png,.jpg,.jpeg,.txt"
        className="sr-only"
        disabled={disabled}
        onChange={choose}
        ref={inputRef}
        type="file"
      />
      {progress !== null && (
        <div className="mt-4" role="status">
          <div className="h-2 overflow-hidden rounded-full bg-slate-200">
            <div className="h-full bg-brand-600 transition-all" style={{ width: `${progress}%` }} />
          </div>
          <p className="mt-1 text-xs text-slate-600">Uploading {progress}%</p>
        </div>
      )}
    </div>
  )
}

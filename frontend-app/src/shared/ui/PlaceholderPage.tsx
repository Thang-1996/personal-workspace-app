import { EmptyState } from './States'

export function PlaceholderPage({ title }: { title: string }) {
  return (
    <section>
      <h1 className="mb-6 text-2xl font-bold text-slate-950">{title}</h1>
      <EmptyState message={`${title} workspace is ready for the next feature story.`} />
    </section>
  )
}

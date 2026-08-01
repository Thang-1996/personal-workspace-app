import { create } from 'zustand'

type UiState = {
  sidebarOpen: boolean
  selectedTaskId: string | null
  closeSidebar: () => void
  toggleSidebar: () => void
  selectTask: (taskId: string | null) => void
}

export const useUiStore = create<UiState>((set) => ({
  sidebarOpen: false,
  selectedTaskId: null,
  closeSidebar: () => set({ sidebarOpen: false }),
  toggleSidebar: () => set((state) => ({ sidebarOpen: !state.sidebarOpen })),
  selectTask: (selectedTaskId) => set({ selectedTaskId }),
}))

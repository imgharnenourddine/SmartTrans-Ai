export interface Client {
  id: string
  companyName: string
  address: string
  phone: string
  email: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateClientRequest {
  companyName: string
  address: string
  phone: string
  email?: string | null
}

export interface UpdateClientRequest {
  companyName?: string
  address?: string
  phone?: string
  email?: string | null
}

export function matchesClientSearch(client: Client, search: string): boolean {
  const query = search.trim().toLowerCase()
  if (!query) {
    return true
  }
  const haystack = [client.companyName, client.address, client.phone, client.email]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()
  return haystack.includes(query)
}

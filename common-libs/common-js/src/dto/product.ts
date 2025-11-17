export type ProductStatus = "DRAFT" | "ACTIVE" | "ARCHIVED";

export interface Image {
    id?: string;
    url: string;
    alt?: string;
    width?: number;
    height?: number;
}

export interface ImageRef {
    externalUrl?: string;
    uploadId?: string;
    alt?: string;
}

export interface Product {
    id: string;
    name: string;
    description?: string;
    sku: string;
    price: number;
    currency: string;
    status: ProductStatus;
    categoryId?: string | null;
    brand?: string;
    tags?: string[];
    metadata?: Record<string, unknown>;
    defaultVariantId?: string | null;
    images?: Image[];
    createdAt: string;
    updatedAt?: string;
}

export interface ProductCreate {
    name: string;
    description?: string;
    sku: string;
    price: number;
    currency: string;
    status?: Extract<ProductStatus, "DRAFT" | "ACTIVE">;
    categoryId?: string;
    brand?: string;
    tags?: string[];
    metadata?: Record<string, unknown>;
    images?: ImageRef[];
}

export interface ProductUpdate {
    name?: string;
    description?: string;
    price?: number;
    currency?: string;
    status?: ProductStatus;
    categoryId?: string;
    brand?: string;
    tags?: string[];
    metadata?: Record<string, unknown>;
    images?: ImageRef[];
}

export type VariantStatus = "AVAILABLE" | "OUT_OF_STOCK" | "DISCONTINUED";

export interface Variant {
    id: string;
    productId: string;
    sku: string;
    attributes?: Record<string, string>;
    price: number;
    listPrice?: number | null;
    currency: string;
    stock?: number;
    status: VariantStatus;
    images?: Image[];
    createdAt: string;
    updatedAt?: string;
}

export interface VariantCreate {
    sku: string;
    productId: string;
    attributes?: Record<string, string>;
    price: number;
    listPrice?: number;
    currency: string;
    stock?: number;
    status?: Extract<VariantStatus, "AVAILABLE" | "OUT_OF_STOCK">;
    images?: ImageRef[];
}

export interface Category {
    id: string;
    name: string;
    slug?: string;
    description?: string;
    parentId?: string | null;
    createdAt: string;
}

export interface PagedProducts {
    items: Product[];
    page: number;
    perPage: number;
    total: number;
}

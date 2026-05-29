export interface Part {
  id: string | number;
  partCode: string;
  partName: string;
  source: string;
  officialPartNo?: string;
  defaultBarcode?: string;
  model?: string;
  categoryCode: string;
  retailPrice?: number;
}

export interface PartLookupResult {
  matched: boolean;
  partId?: string | number;
  partCode?: string;
  partName?: string;
  officialPartNo?: string;
  defaultBarcode?: string;
  source?: string;
  name?: string;
  model?: string;
  categoryCode?: string;
  category?: string;
  costPrice?: number;
  enabled?: boolean;
  actualQty?: number;
  availableQty?: number;
  reservedQty?: number;
}

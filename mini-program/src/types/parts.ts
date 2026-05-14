export interface Part {
  id: string | number;
  partCode: string;
  partName: string;
  source: string;
  officialPartNo?: string;
  model?: string;
  categoryCode: string;
  retailPrice?: number;
}

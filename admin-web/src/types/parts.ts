export interface PartRecord {
  id: number;
  storeId: number;
  partCode: string;
  officialPartNo: string;
  partName: string;
  model: string;
  source: string;
  categoryCode: string;
  referenceCostPrice: number;
  defaultBarcode: string;
  locationRemark: string;
  createSource: string;
  status: string;
  remark: string;
}

export interface PartQuery {
  partCode?: string;
  partName?: string;
  source?: string;
  enabled?: boolean;
  pageNo: number;
  pageSize: number;
}

export interface CreateThirdPartyPartBody {
  partName: string;
  model?: string;
  categoryCode?: string;
  referenceCostPrice?: number;
  locationRemark?: string;
  remark?: string;
}

export interface CreateOfficialPartBody {
  partName: string;
  officialPartNo: string;
  model?: string;
  categoryCode?: string;
  referenceCostPrice?: number;
  locationRemark?: string;
  remark?: string;
}

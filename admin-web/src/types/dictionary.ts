export interface DictionaryItem {
  itemCode: string;
  itemName: string;
  sortOrder: number;
  enabled: boolean;
}

export interface DictionaryQuery {
  dictCode?: string;
  dictLabel?: string;
  enabled?: boolean;
}

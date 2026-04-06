using System;

namespace RnDLaboratoryStock.Core.Models;

public partial class RndIndoorLaboratoryMasterDeleted
{
    public int Id { get; set; }

    public int OldId { get; set; }

    public string? Epc { get; set; }

    public string ProductNumber { get; set; } = null!;

    public DateTime? ExpiredDate { get; set; }

    public string CabinetNumber { get; set; } = null!;

    public int Shelf { get; set; }

    public string Quantity { get; set; } = null!;

    public string Brand { get; set; } = null!;

    public string Code { get; set; } = null!;

    public string PackageQuantity { get; set; } = null!;

    public string? Unit { get; set; }

    public string? ChemicalName { get; set; }

    public string? SupplierName { get; set; }

    public string? QuantityNumber { get; set; }

    public int? Limit { get; set; }

    public int? ExpireLimit { get; set; }

    public DateTime InsertedAt { get; set; }

    public DateTime DeletedAt { get; set; }

    public string WmCode { get; set; } = null!;
}

using System;

namespace RnDLaboratoryStock.Core.Models;

public partial class RndIndoorLaboratoryMaster
{
    public int Id { get; set; }

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
}

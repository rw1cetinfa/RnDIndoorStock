using System;

namespace RnDLaboratoryStock.Core.Models;

public partial class RndIndoorLaboratoryMaterial
{
    public int Id { get; set; }

    public string? Epc { get; set; }

    public string Brand { get; set; } = null!;

    public string BrandCode { get; set; } = null!;

    public double Amount { get; set; }

    public string Unit { get; set; } = null!;

    public string ChemicalName { get; set; } = null!;

    public string CompanyName { get; set; } = null!;

    public int ItemNumber { get; set; }

    public int Remain { get; set; }

    public int? Limit { get; set; }

    public DateTime InsertedAt { get; set; }

    public DateTime? Exp { get; set; }
}

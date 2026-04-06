using System;

namespace RnDLaboratoryStock.Core.Models;

public partial class RndIndoorLaboratorySession
{
    public int Id { get; set; }

    public string WmCode { get; set; } = null!;

    public int LatestShelfNumber { get; set; }

    public string LatestCabinetNumber { get; set; } = null!;

    public bool IsActive { get; set; }

    public DateTime? InsertedAt { get; set; }

    public DateTime? ClosedAt { get; set; }
}

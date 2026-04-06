using System;

namespace RnDLaboratoryStock.Core.Models;

public partial class RndIndoorLaboratoryMailList
{
    public int Id { get; set; }

    public string Email { get; set; } = null!;

    public DateTime InsertedAt { get; set; }
}

namespace RnDLaboratoryStock.Core.Models;

public partial class RndIndoorLaboratoryAndroidVersion
{
    public int Id { get; set; }

    public string AppVersion { get; set; } = null!;

    public bool IsActive { get; set; }

    public DateTime InsertedAt { get; set; }
}

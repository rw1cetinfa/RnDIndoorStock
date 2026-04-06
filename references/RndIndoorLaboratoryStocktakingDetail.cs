namespace RnDLaboratoryStock.Core.Models;

public partial class RndIndoorLaboratoryStocktakingDetail
{
    public int Id { get; set; }

    public int ParentId { get; set; }

    public string Epc { get; set; } = null!;
}
